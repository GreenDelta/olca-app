package org.openlca.app.tools.openepd.export;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.DoubleCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.ModelType;
import org.openlca.io.openepd.mapping.IndicatorMapping;
import org.openlca.io.openepd.mapping.MethodMapping;
import org.openlca.io.openepd.mapping.Vocab.Indicator;
import org.openlca.io.openepd.mapping.Vocab.Method;
import org.openlca.util.Strings;

import java.util.Arrays;
import java.util.stream.Stream;

record MappingSection(MethodMapping model) {

	void render(Composite body, FormToolkit tk) {
		var title = model.method() != null
			? "Results: " + Labels.name(model.method())
			: "Results";
		var section = UI.section(body, tk, title);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		createCombo(comp, tk);
		createTable(comp);
	}

	private void createCombo(Composite parent, FormToolkit tk) {
		var comp = UI.formComposite(parent, tk);
		var combo = UI.formCombo(comp, tk, "openEPD LCIA Method");
		var methods = Method.values();
		var items = new String[methods.length];
		int selectionIdx = -1;
		Method selected = null;
		double score = 0;
		for (int i = 0; i < methods.length; i++) {
			var method = methods[i];
			items[i] = method.code();
			if (method == Method.UNKNOWN_LCIA
				&& selected == null) {
				selectionIdx = i;
				selected = method;
				continue;
			}
			if (model.method() != null) {
				var s = method.matchScoreOf(model.method().name);
				if (s > score) {
					selectionIdx = i;
					selected = method;
					score = s;
				}
			}
		}

		model.epdMethod(selected);
		combo.setItems(items);
		combo.select(selectionIdx);
		Controls.onSelect(combo, $ -> {
			var idx = combo.getSelectionIndex();
			model.epdMethod(methods[idx]);
		});
	}

	private void createTable(Composite parent) {
		var columns = new String[5 + model.scopes().size()];
		columns[0] = "Indicator";
		columns[1] = "Unit";
		columns[2] = "openEPD Indicator";
		columns[3] = "openEPD Unit";
		columns[4] = "Factor";
		for (int i = 0; i < model.scopes().size(); i++) {
			columns[i + 5] = model.scopes().get(i);
		}
		var table = Tables.createViewer(parent, columns);
		table.setLabelProvider(new TableLabel());
		var widths = new double[columns.length];
		widths[0] = 0.2;
		widths[1] = 0.1;
		widths[2] = 0.1;
		widths[3] = 0.1;
		widths[4] = 0.1;
		Arrays.fill(widths, 5, columns.length,
			.4 / (model.scopes().size()));
		Tables.bindColumnWidths(table, widths);
		for (int i = 4; i < columns.length; i++) {
			table.getTable()
				.getColumn(i)
				.setAlignment(SWT.CENTER);
		}
		table.setInput(model.entries());

		var modifier = new ModifySupport<IndicatorMapping>(table)
			.bind("Factor", new FactorColumn())
			.bind("openEPD Indicator", new IndicatorColumn());
		for (var scope : model.scopes()) {
			modifier.bind(scope, new ScopeColumn(scope));
		}
	}

	private class TableLabel extends LabelProvider
		implements ITableLabelProvider, ITableColorProvider {

		@Override
		public Color getForeground(Object obj, int col) {
			if (!(obj instanceof IndicatorMapping row))
				return null;
			if (col == 1 || col == 3 || col == 4) {
				if (row.epdIndicator() != null && row.unit() == null)
					return Colors.fromHex("#ff5722");
			}
			return null;
		}

		@Override
		public Color getBackground(Object obj, int col) {
			return null;
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0
				? Images.get(ModelType.IMPACT_CATEGORY)
				: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof IndicatorMapping row))
				return null;
			var epdInd = row.epdIndicator();
			return switch (col) {
				case 0 -> Labels.name(row.indicator());
				case 1 -> row.indicator() != null
					? row.indicator().referenceUnit
					: null;
				case 2 -> epdInd != null
					? epdInd.code() + " - " + epdInd.description()
					: " - ";
				case 3 -> epdInd != null ? epdInd.unit() : " - ";
				case 4 -> epdInd != null
					? Double.toString(row.factor())
					: " - ";
				default -> {
					int idx = col - 5;
					if (idx < 0 || idx >= model.scopes().size())
						yield " - ";
					var scope = model.scopes().get(idx);
					var value = row.values().get(scope);
					yield value == null
						? " - "
						: Numbers.format(value * row.factor());
				}
			};
		}
	}

	private static class FactorColumn extends DoubleCellModifier<IndicatorMapping> {

		@Override
		public Double getDouble(IndicatorMapping row) {
			return row.factor();
		}

		@Override
		public void setDouble(IndicatorMapping row, Double value) {
			row.factor(value != null ? value : 1.0);
		}
	}

	private static class ScopeColumn extends DoubleCellModifier<IndicatorMapping> {

		private final String scope;

		ScopeColumn(String scope) {
			this.scope = scope;
		}

		@Override
		public Double getDouble(IndicatorMapping row) {
			return row.values().get(scope);
		}

		@Override
		public void setDouble(IndicatorMapping row, Double value) {
			row.values().put(scope, value);
		}
	}

	private static class IndicatorColumn
		extends ComboBoxCellModifier<IndicatorMapping, Indicator> {

		@Override
		protected Indicator[] getItems(IndicatorMapping row) {
			return Stream.concat(
					Stream.of((Indicator) null),
					Stream.of(Indicator.values()))
				.sorted((i1, i2) -> {
						if (i1 == null && i2 == null)
							return 0;
						if (i1 == null)
							return -1;
						if (i2 == null)
							return 1;
						return i1.type() != i2.type()
							? i1.type().ordinal() - i2.type().ordinal()
							: Strings.compare(i1.code(), i2.code());
					}
				).toArray(Indicator[]::new);
		}

		@Override
		protected Indicator getItem(IndicatorMapping row) {
			return row.epdIndicator();
		}

		@Override
		protected String getText(Indicator i) {
			return i == null
				? ""
				: i.code() + " - " + i.description();
		}

		@Override
		protected void setItem(IndicatorMapping row, Indicator i) {
			if (row == null)
				return;
			if (i == null) {
				row.epdIndicator(null)
					.unit(null)
					.factor(1.0);
				return;
			}
			var unit = row.indicator() != null
				? i.unitMatchOf(row.indicator().referenceUnit).orElse(null)
				: null;
			var factor = unit != null
				? unit.factor()
				: 1.0;
			row.epdIndicator(i)
				.unit(unit)
				.factor(factor);
		}
	}
}
