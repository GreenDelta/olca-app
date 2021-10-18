package org.openlca.app.editors.results.openepd.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.editors.results.openepd.model.Ec3ImpactModel;
import org.openlca.app.editors.results.openepd.model.Ec3ImpactSet;
import org.openlca.app.editors.results.openepd.model.Ec3Measurement;
import org.openlca.app.editors.results.openepd.model.Ec3ScopeSet;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ResultModel;
import org.openlca.util.Pair;
import org.openlca.util.Strings;

class ResultSection {

	private final ExportDialog dialog;
	private final ResultModel result;

	private String selectedScope = "A1A2A3";
	private Ec3ImpactModel.Method selectedMethod;
	private Section section;
	private final List<MappedValue> mappedValues = new ArrayList<>();

	private ResultSection(ExportDialog dialog, ResultModel result) {
		this.dialog = dialog;
		this.result = result;

		// select the best matching EC3 LCIA method for the LCIA method
		// of the setup
		var impactModel = dialog.impactModel;
		if (result.setup != null && result.setup.impactMethod() != null) {
			var resultName = result.setup.impactMethod().name;
			int score = 0;
			for (var next : impactModel.methods()) {
				var nextScore = Ec3ImpactModel.mapScore(resultName, next.keywords());
				if (nextScore > score) {
					score = nextScore;
					selectedMethod = next;
				}
			}
		}

		if (selectedMethod != null) {
			mappedValues.addAll(MappedValue.initAll(selectedMethod, result));
		}
	}

	static ResultSection of(ExportDialog dialog, ResultModel result) {
		return new ResultSection(dialog, result);
	}

	Pair<String, Ec3ImpactSet> createImpacts() {
		if (selectedMethod == null)
			return null;
		var impactSet = new Ec3ImpactSet();
		for (var val : mappedValues) {
			var epdIndicator = val.epdIndicator;
			var scopeSet = new Ec3ScopeSet();
			scopeSet.put(selectedScope, Ec3Measurement.of(
				val.amount, epdIndicator.unit()));
			impactSet.put(epdIndicator.id(), scopeSet);
		}
		return Pair.of(selectedMethod.id(), impactSet);
	}

	void render(Composite body, FormToolkit tk) {
		section = UI.section(body, tk, M.GeneralInformation);
		updateSectionTitle();
		var comp = UI.sectionClient(section, tk, 1);

		var top = tk.createComposite(comp);
		UI.gridData(top, true, false);
		UI.gridLayout(top, 2, 10, 0);

		var methodCombo = new EntityCombo<>(
			UI.formCombo(top, tk, "OpenEPD LCIA Method"),
			dialog.impactModel.methods(),
			m -> Strings.orEmpty(m.id()))
			.select(selectedMethod);

		var scopes = List.of(
			"A1A2A3", "A1", "A2", "A3", "A4", "A5",
			"B1", "B2", "B3", "B4", "B5", "B6", "B7",
			"C1", "C2", "C3", "C4");
		new EntityCombo<>(
			UI.formCombo(top, tk, "Scope"), scopes, Objects::toString)
			.select(selectedScope)
			.onSelected(scope -> {
				selectedScope = scope;
				updateSectionTitle();
			});

		var table = Tables.createViewer(comp,
			"EPD Indicator",
			"EPD Unit",
			"Amount",
			"Result Indicator",
			"Result Unit");
		table.setLabelProvider(new TableLabel());
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);
		table.setInput(mappedValues);

		methodCombo.onSelected(method -> {
			selectedMethod = method;
			updateSectionTitle();
			mappedValues.clear();
			mappedValues.addAll(MappedValue.initAll(method, result));
			table.setInput(mappedValues);
		});
	}

	private void updateSectionTitle() {
		if (section == null)
			return;
		var title = selectedMethod != null
			? selectedScope + " - " + selectedMethod.name()
			: selectedScope;
		section.setText(title);
	}

	private static class MappedValue {

		final Ec3ImpactModel.Indicator epdIndicator;
		double amount;
		ImpactCategory resultIndicator;

		MappedValue(Ec3ImpactModel.Indicator epdIndicator) {
			this.epdIndicator = epdIndicator;
		}

		static List<MappedValue> initAll(
			Ec3ImpactModel.Method selectedMethod, ResultModel result) {

			if (selectedMethod == null
				|| result.setup == null
				|| result.setup.impactMethod() == null)
				return Collections.emptyList();

			var map = selectedMethod.matchIndicators(result);
			var items = new ArrayList<MappedValue>();
			for (var epdIndicator : selectedMethod.indicators()) {
				var value = new MappedValue(epdIndicator);
				var impact = map.get(epdIndicator.id());
				if (impact != null) {
					value.amount = impact.amount;
					value.resultIndicator = impact.indicator;
				}
				items.add(value);
			}

			items.sort((item1, item2) -> Strings.compare(
				item1.epdIndicator.id(), item2.epdIndicator.id()));
			return items;
		}
	}

	private static class TableLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return switch (col) {
				case 0 -> Icon.BUILDING.get();
				case 3 -> Images.get(ModelType.IMPACT_CATEGORY);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof MappedValue value))
				return null;
			return switch (col) {
				case 0 -> {
					var i = value.epdIndicator;
					yield i.id().toUpperCase() + " - " + i.name();
				}
				case 1 -> value.epdIndicator.name();
				case 2 -> Numbers.format(value.amount);
				case 3 -> value.resultIndicator != null
					? Labels.name(value.resultIndicator)
					: null;
				case 4 -> value.resultIndicator != null
					? value.resultIndicator.referenceUnit
					: null;
				default -> null;
			};
		}
	}

}
