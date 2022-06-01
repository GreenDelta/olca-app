package org.openlca.app.tools.openepd;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.DoubleCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.ImpactCategory;
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

	public static MappingTable forImport(MethodMapping mapping) {
		return new MappingTable(mapping, true);
	}

	public static MappingTable forExport(MethodMapping mapping) {
		return new MappingTable(mapping, false);
	}

	public TableViewer create(Composite parent) {
		var table = Tables.createViewer(parent, headers());
		table.setLabelProvider(MappingLabel.of(this));
		Tables.bindColumnWidths(table, widths());
		for (int i = 4; i < columnCount(); i++) {
			table.getTable()
				.getColumn(i)
				.setAlignment(SWT.CENTER);
		}
		table.setInput(mapping.entries());

		// bind modifiers
		var modifier = new ModifySupport<IndicatorMapping>(table)
			.bind("Factor", new FactorColumn());
		if (isForImport) {
			modifier.bind("Indicator", new IndicatorColumn());
		} else {
			modifier.bind("openEPD Indicator", new EpdIndicatorColumn());
		}
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

	private class IndicatorColumn extends
		ComboBoxCellModifier<IndicatorMapping, ImpactCategory> {

		@Override
		protected ImpactCategory[] getItems(IndicatorMapping row) {
			var method = mapping.method();
			if (method == null)
				return new ImpactCategory[0];
			var impacts = method.impactCategories;
			impacts.sort(Comparator.comparing(Labels::name));
			var array = new ImpactCategory[impacts.size() + 1];
			for (int i = 0; i < impacts.size(); i++) {
				array[i + 1] = impacts.get(i);
			}
			return array;
		}

		@Override
		protected ImpactCategory getItem(IndicatorMapping row) {
			return row.indicator();
		}

		@Override
		protected String getText(ImpactCategory impact) {
			return Labels.name(impact);
		}

		@Override
		protected void setItem(IndicatorMapping row, ImpactCategory impact) {
			if (Objects.equals(impact, row.indicator()))
				return;
			var epdInd = row.epdIndicator();
			if (epdInd == null)
				return;

			if (impact == null) {
				row.indicator(null)
					.unit(null)
					.factor(1);
				return;
			}

			var unit = epdInd.unitMatchOf(impact.referenceUnit).orElse(null);
			row.indicator(impact)
				.unit(unit)
				.factor(unit != null
					? 1 / unit.factor()
					: 1);
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
