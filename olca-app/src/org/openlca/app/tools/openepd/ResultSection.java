package org.openlca.app.tools.openepd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.openepd.model.Ec3ImpactModel;
import org.openlca.app.tools.openepd.model.Ec3ImpactResult;
import org.openlca.app.tools.openepd.model.Ec3IndicatorResult;
import org.openlca.app.tools.openepd.model.Ec3Measurement;
import org.openlca.app.tools.openepd.model.Ec3ScopeValue;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.DoubleCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;
import org.openlca.util.Strings;

public class ResultSection {

	private final EpdEditor editor;
	private final Result result;

	private String selectedScope = "A1A2A3";
	private Ec3ImpactModel.Method selectedMethod;
	private Section section;
	private Consumer<ResultSection> onDelete;
	private final List<MappedValue> mappedValues = new ArrayList<>();

	private ResultSection(EpdEditor editor, Result result) {
		this.editor = editor;
		this.result = result;

		// select the best matching EC3 LCIA method for the LCIA method
		// of the setup
		var impactModel = editor.impactModel;
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

	static ResultSection of(EpdEditor editor, Result result) {
		return new ResultSection(editor, result);
	}

	ResultSection onDelete(Consumer<ResultSection> onDelete) {
		this.onDelete = onDelete;
		return this;
	}

	Ec3ImpactResult createEpdResult() {
		if (selectedMethod == null)
			return null;
		var indicatorResults = new ArrayList<Ec3IndicatorResult>();
		for (var val : mappedValues) {
			var epdIndicator = val.epdIndicator;
			var value = Ec3Measurement.of(val.amount, epdIndicator.unit());
			var scopeValue = new Ec3ScopeValue(selectedScope, value);
			var result = Ec3IndicatorResult.of((epdIndicator.id()));
			result.values().add(scopeValue);
			indicatorResults.add(result);
		}
		return new Ec3ImpactResult(selectedMethod.id(), indicatorResults);
	}

	ResultSection render(Composite body, FormToolkit tk) {
		section = UI.section(body, tk, M.GeneralInformation);
		UI.fillHorizontal(section);
		updateSectionTitle();
		var comp = UI.sectionClient(section, tk, 1);

		var top = tk.createComposite(comp);
		UI.fillHorizontal(top);
		UI.gridLayout(top, 2, 10, 0);

		// the result link
		UI.formLabel(top, tk, M.Result);
		var link = tk.createImageHyperlink(top, SWT.NONE);
		link.setImage(Images.get(ModelType.RESULT));
		link.setText(Labels.name(result));
		Controls.onClick(link, $ -> App.open(result));

		// create the method combo
		var methodCombo = new EntityCombo<>(
			UI.formCombo(top, tk, "openEPD LCIA Method"),
			editor.impactModel.methods(),
			m -> Strings.orEmpty(m.id()))
			.select(selectedMethod);

		// create the scope combo
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

		// create the table
		var table = Tables.createViewer(comp,
			"EPD Indicator",
			"EPD Unit",
			"Amount",
			"Result Indicator",
			"Result Unit");
		table.setLabelProvider(new TableLabel());
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);
		table.setInput(mappedValues);
		new ModifySupport<MappedValue>(table)
			.bind("Amount", new AmountModifier())
			.bind("Result Indicator", new ImpactModifier(result));

		// add table actions
		var onDeleteRow = Actions.onRemove(() -> {
			MappedValue mv = Viewers.getFirstSelected(table);
			if (mv != null) {
				mappedValues.remove(mv);
				table.setInput(mappedValues);
			}
		});
		var onReInitRows = Actions.create(
			"Reload", Icon.REFRESH.descriptor(), () -> {
				mappedValues.clear();
				mappedValues.addAll(MappedValue.initAll(selectedMethod, result));
				table.setInput(mappedValues);
			});
		Actions.bind(table, onReInitRows, onDeleteRow);

		// handle method changes
		methodCombo.onSelected(method -> {
			selectedMethod = method;
			updateSectionTitle();
			mappedValues.clear();
			mappedValues.addAll(MappedValue.initAll(method, result));
			table.setInput(mappedValues);
		});

		// add the delete action
		var selfDelete = Actions.onRemove(() -> {
			section.dispose();
			if (onDelete!= null) {
				onDelete.accept(this);
			}
		});
		Actions.bind(section, selfDelete);

		return this;
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
			Ec3ImpactModel.Method selectedMethod, Result result) {

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

	private static class ImpactModifier extends
		ComboBoxCellModifier<MappedValue, ImpactCategory> {

		private final Result result;

		ImpactModifier(Result result) {
			this.result = result;
		}

		@Override
		protected ImpactCategory[] getItems(MappedValue mv) {
			var empty = new ImpactCategory[0];
			if (result == null
				|| result.setup == null
				|| result.setup.impactMethod() == null)
				return empty;

			var method = result.setup.impactMethod();
			var impacts = method.impactCategories.toArray(empty);
			Arrays.sort(impacts,
				(i1, i2) -> Strings.compare(Labels.name(i1), Labels.name(i2)));
			return impacts;
		}

		@Override
		protected ImpactCategory getItem(MappedValue mv) {
			return mv.resultIndicator;
		}

		@Override
		protected String getText(ImpactCategory impact) {
			return Labels.name(impact);
		}

		@Override
		protected void setItem(MappedValue mv, ImpactCategory impact) {
			mv.resultIndicator = impact;
			mv.amount = impact == null
				? 0
				: result.impacts.stream()
				.filter(r -> Objects.equals(r.indicator, impact))
				.mapToDouble(r -> r.amount)
				.findAny()
				.orElse(0);
		}
	}

	private static class AmountModifier extends DoubleCellModifier<MappedValue> {

		@Override
		public Double getDouble(MappedValue mv) {
			return mv.amount;
		}

		@Override
		public void setDouble(MappedValue mv, Double value) {
			mv.amount = value == null
				? 0
				: value;
		}
	}
}
