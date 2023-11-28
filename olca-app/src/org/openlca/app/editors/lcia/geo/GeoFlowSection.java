package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
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
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.geo.lcia.GeoFactorCalculator;
import org.openlca.geo.lcia.GeoFlowBinding;
import org.openlca.io.CategoryPath;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
		var remove = Actions.onRemove(this::onRemove);
		var calc = Actions.onCalculate(this::onCalculate);
		Actions.bind(table, add, remove, calc);
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

	private void onCalculate() {
		var setup = page.setup;
		if (setup == null) {
			MsgBox.error("Invalid calculation setup",
					"No GeoJSON file is selected.");
			return;
		}
		if (setup.bindings.isEmpty()) {
			MsgBox.error("Invalid calculation setup",
					"No flow bindings are defined.");
			return;
		}
		if (setup.features.isEmpty()) {
			MsgBox.error("Invalid calculation setup",
					"Could not find geographic features.");
			return;
		}

		// select the locations
		var locs = ModelSelector.multiSelect(ModelType.LOCATION);
		if (locs.isEmpty())
			return;
		var locDao = new LocationDao(Database.get());
		var locations = locs.stream()
				.map(d -> locDao.getForId(d.id))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		var calc = GeoFactorCalculator.of(
				Database.get(), page.setup, locations);
		var factors = new AtomicReference<List<ImpactFactor>>();
		App.runWithProgress("Calculate regionalized factors",
				() -> factors.set(calc.calculate()),
				() -> GeoFactorDialog.open(page, factors.get()));
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
