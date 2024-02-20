package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FormulaCellEditor;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.geo.lcia.GeoFactorCalculator;
import org.openlca.geo.lcia.GeoFlowBinding;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

class GeoFlowSection {

	private final GeoPage page;
	private TableViewer table;

	GeoFlowSection(GeoPage page) {
		this.page = page;
	}

	void drawOn(Composite body, FormToolkit tk) {

		// create the section
		Section section = UI.section(body, tk, "Flow bindings");
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		// create the table
		table = Tables.createViewer(comp,
				M.Flow,
				M.Category,
				M.Formula,
				"Default value",
				M.Unit);
		table.setLabelProvider(new Label());
		Tables.bindColumnWidths(
				table, 0.2, 0.2, 0.2, 0.2, 0.2);
		new ModifySupport<GeoFlowBinding>(table)
				.bind(M.Formula, getFormulaEditor());

		// bind actions
		var add = Actions.onAdd(this::onAdd);
		var createForUsed = Actions.create(
				"Create for used flows",
				Icon.EDIT.descriptor(),
				this::onCreateForUsedFlows);
		createForUsed.setToolTipText(
				"Creates flow bindings for all flows used in this impact category");
		var remove = Actions.onRemove(this::onRemove);
		var calc = Actions.onCalculate(this::onCalculate);
		var calcMissing = Actions.create(
				"Calculate all missing locations",
				Icon.ANALYSIS_RESULT.descriptor(),
				this::onCalculateAllMissing);
		Actions.bind(table, add, createForUsed, remove, calc, calcMissing);
		Actions.bind(section, add, remove, calc);
	}

	private void onRemove() {
		if (page.setup == null)
			return;
		List<GeoFlowBinding> bindings = Viewers.getAllSelected(table);
		if (bindings.isEmpty())
			return;
		page.setup.bindings.removeAll(bindings);
		table.setInput(page.setup.bindings);
	}

	private void onAdd() {
		if (page.setup == null)
			return;
		var flows = ModelSelector.multiSelect(ModelType.FLOW);
		if (flows.isEmpty())
			return;
		var dao = new FlowDao(Database.get());
		for (var d : flows) {
			boolean isPresent = false;
			for (GeoFlowBinding b : page.setup.bindings) {
				if (b.flow == null)
					continue;
				if (Objects.equals(b.flow.refId, d.refId)) {
					isPresent = true;
					break;
				}
			}
			if (isPresent)
				continue;
			Flow flow = dao.getForId(d.id);
			if (flow == null)
				continue;
			GeoFlowBinding b = new GeoFlowBinding(flow);
			page.setup.bindings.add(b);
		}
		table.setInput(page.setup.bindings);
	}

	private void onCreateForUsedFlows() {
		Set<Flow> flows = App.exec("Collect flows", () -> {
			if (page.setup == null)
				return Collections.emptySet();
			var existing = page.setup.bindings.stream()
					.filter(b -> b.flow != null)
					.map(b -> b.flow)
					.collect(Collectors.toSet());
			var impact = page.editor.getModel();
			var fs = new HashSet<Flow>();
			for (var f : impact.impactFactors) {
				if (f.flow == null || existing.contains(f.flow))
					continue;
				fs.add(f.flow);
			}
			return fs;
		});

		if (flows.isEmpty()) {
			MsgBox.info("No flows found",
					"There are no flows in the characterization " +
							"factors that do not have a binding yet");
			return;
		}

		var dialog = new InputDialog(UI.shell(),
				"Provide a default formula",
				"Please provide a default formula that " +
						"should be used for the new bindings",
				"1", null);
		if (dialog.open() != Window.OK)
			return;
		var formula = dialog.getValue();
		if (Strings.nullOrEmpty(formula)) {
			formula = "1";
		}

		for (var flow : flows) {
			var binding = new GeoFlowBinding(flow);
			binding.formula = formula;
			page.setup.bindings.add(binding);
		}
		table.setInput(page.setup.bindings);
	}

	private void onCalculate() {
		if (!canCalculate())
			return;

		// select the locations
		var locs = ModelSelector.multiSelect(ModelType.LOCATION);
		if (locs.isEmpty())
			return;
		var locDao = new LocationDao(Database.get());
		var locations = locs.stream()
				.map(d -> locDao.getForId(d.id))
				.filter(Objects::nonNull)
				.toList();

		runCalculation(locations);
	}

	private void onCalculateAllMissing() {
		if (!canCalculate())
			return;
		var locations = App.exec("Collect locations", () -> {
			var dao = new LocationDao(Database.get());
			var used = page.editor.getModel()
					.impactFactors.stream()
					.filter(f -> f.location != null)
					.map(f -> f.location.id)
					.collect(Collectors.toSet());
			return dao.getDescriptors()
					.stream()
					.filter(d -> !used.contains(d.id))
					.map(d -> dao.getForId(d.id))
					.filter(loc -> loc.geodata != null && loc.geodata.length > 0)
					.toList();
		});

		if (locations.isEmpty()) {
			MsgBox.info(
					"No locations found",
					"No locations with geo-data which are not already " +
							"present in the CFs could be found.");
			return;
		}
		var b = Question.ask("Run calculation?",
				"Calculate factors for " + locations.size() + " additional locations?");
		if (b) {
			runCalculation(locations);
		}
	}

	private void runCalculation(List<Location> locations) {
		var calc = GeoFactorCalculator.of(
				Database.get(), page.setup, locations);
		var factors = new AtomicReference<List<ImpactFactor>>();
		App.runWithProgress("Calculate regionalized factors",
				() -> factors.set(calc.calculate()),
				() -> GeoFactorDialog.open(page, factors.get()));
	}

	private boolean canCalculate() {
		var setup = page.setup;
		if (setup == null) {
			MsgBox.error("Invalid calculation setup",
					"No GeoJSON file is selected.");
			return false;
		}
		if (setup.bindings.isEmpty()) {
			MsgBox.error("Invalid calculation setup",
					"No flow bindings are defined.");
			return false;
		}
		if (setup.features.isEmpty()) {
			MsgBox.error("Invalid calculation setup",
					"Could not find geographic features.");
			return false;
		}
		return true;
	}

	void update() {
		if (page.setup == null)
			return;
		table.setInput(page.setup.bindings);
	}

	private FormulaCellEditor getFormulaEditor() {

		// TODO: it would be good to remove global
		// parameters from the auto-completion here
		// or, allow all parameters (also from the
		// indicator) here
		var editor = new FormulaCellEditor(table, () -> {
			if (page.setup == null)
				return Collections.emptyList();
			return page.setup.properties.stream()
					.map(gp -> {
						var p = new Parameter();
						p.name = gp.identifier;
						return p;
					})
					.collect(Collectors.toList());
		});

		editor.onEdited((obj, formula) -> {
			if (!(obj instanceof GeoFlowBinding binding))
				return;
			binding.formula = formula;
			table.refresh();
		});
		return editor;
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof GeoFlowBinding b))
				return null;
			if (b.flow == null)
				return null;
			return switch (col) {
				case 0 -> Images.get(b.flow);
				case 1 -> Images.get(b.flow.category);
				case 2 -> Icon.EDIT.get();
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {

			if (!(obj instanceof GeoFlowBinding b))
				return null;
			if (b.flow == null)
				return null;

			return switch (col) {
				case 0 -> Labels.name(b.flow);
				case 1 -> CategoryPath.getFull(b.flow.category);
				case 2 -> b.formula;
				case 3 -> {
					var defVal = b.defaultValueOf(page.setup.properties);
					yield defVal != null
							? Numbers.format(defVal)
							: "FORMULA ERROR";
				}
				case 4 -> Labels.name(b.flow.getReferenceUnit());
				default -> null;
			};
		}
	}
}
