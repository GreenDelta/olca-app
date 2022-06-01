package org.openlca.app.tools.openepd;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.DoubleCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.ModelType;
import org.openlca.io.openepd.io.IndicatorMapping;
import org.openlca.io.openepd.io.MethodMapping;
import org.openlca.io.openepd.io.Vocab;
import org.openlca.util.Strings;

public record MappingTable(
	MethodMapping mapping,
	boolean isForImport,
	int columnCount) {

	private MappingTable(MethodMapping mapping, boolean isForImport) {
		this(mapping, isForImport, mapping.scopes().size() + 5);
	}

	public MappingTable forImport(MethodMapping mapping) {
		return new MappingTable(mapping, true);
	}

	public MappingTable forExport(MethodMapping mapping) {
		return new MappingTable(mapping, false);
	}

	public TableViewer create(Composite parent) {
		var table = Tables.createViewer(parent, headers());
		table.setLabelProvider(new TableLabel());
		Tables.bindColumnWidths(table, widths());
		for (int i = 4; i < columnCount(); i++) {
			table.getTable()
				.getColumn(i)
				.setAlignment(SWT.CENTER);
		}
		table.setInput(mapping.entries());

		var modifier = new ModifySupport<IndicatorMapping>(table)
			.bind("Factor", new FactorColumn())
			.bind("openEPD Indicator", new EpdIndicatorColumn());
		for (var scope : mapping.scopes()) {
			modifier.bind(scope, new ScopeColumn(scope));
		}
		return table;
	}

	private String[] headers() {
		var columns = new String[columnCount()];
		if (isForImport) {
			columns[0] = "openEPD Indicator";
			columns[1] = "openEPD Unit";
			columns[2] = "Indicator";
			columns[3] = "Unit";
		} else {
			columns[0] = "Indicator";
			columns[1] = "Unit";
			columns[2] = "openEPD Indicator";
			columns[3] = "openEPD Unit";
		}
		columns[4] = "Factor";
		for (int i = 0; i < mapping.scopes().size(); i++) {
			columns[i + 5] = mapping.scopes().get(i);
		}
		return columns;
	}

	private double[] widths() {
		var widths = new double[columnCount];
		widths[0] = isForImport ? 0.1 : 0.2;
		widths[1] = 0.1;
		widths[2] = isForImport ? 0.2 : 0.1;
		widths[3] = 0.1;
		widths[4] = 0.1;
		Arrays.fill(widths, 5, columnCount,
			.4 / (mapping.scopes().size()));
		return widths;
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
					if (idx < 0 || idx >= mapping.scopes().size())
						yield " - ";
					var scope = mapping.scopes().get(idx);
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

	private static class EpdIndicatorColumn
		extends ComboBoxCellModifier<IndicatorMapping, Vocab.Indicator> {

		@Override
		protected Vocab.Indicator[] getItems(IndicatorMapping row) {
			return Stream.concat(
					Stream.of((Vocab.Indicator) null),
					Stream.of(Vocab.Indicator.values()))
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
				).toArray(Vocab.Indicator[]::new);
		}

		@Override
		protected Vocab.Indicator getItem(IndicatorMapping row) {
			return row.epdIndicator();
		}

		@Override
		protected String getText(Vocab.Indicator i) {
			return i == null
				? ""
				: i.code() + " - " + i.description();
		}

		@Override
		protected void setItem(IndicatorMapping row, Vocab.Indicator i) {
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
