package org.openlca.app.editors.epds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.io.openepd.EpdImpactResult;
import org.openlca.io.openepd.EpdIndicatorResult;
import org.openlca.io.openepd.EpdMeasurement;
import org.openlca.io.openepd.EpdScopeValue;
import org.openlca.util.Strings;

class ExportResultSection {

	private final EpdImpactResult result;
	private final String[] mods;

	ExportResultSection(EpdImpactResult result) {
		this.result = result;
		var mods = new HashSet<String>();
		result.indicatorResults()
			.forEach(i -> i.values().forEach(s -> mods.add(s.scope())));
		this.mods = mods.stream().sorted().toArray(String[]::new);
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Results: " + result.method());
		var comp = UI.sectionClient(section, tk, 1);

		var items = Item.allOf(result.indicatorResults(), mods);
		if (Item.hasNonEpdItems(items)) {
			var error = UI.formLabel(comp, tk,
				"Some indicator codes and/or units " +
					"may are not supported by the openEPD API.");
			error.setForeground(Colors.systemColor(SWT.COLOR_RED));
		}

		var columns = new String[mods.length + 2];
		columns[0] = "Indicator";
		columns[1] = "Unit";
		System.arraycopy(mods, 0, columns, 2, mods.length);
		var table = Tables.createViewer(comp, columns);
		double[] colWidths = new double[2 + mods.length];
		Arrays.fill(colWidths, 1.0 / (2.0 + mods.length));
		Tables.bindColumnWidths(table, colWidths);
		table.setLabelProvider(new TableLabel());
		table.setInput(items);
	}

	private static class TableLabel extends LabelProvider
		implements ITableLabelProvider, ITableColorProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			return switch (col) {
				case 0 -> item.indicator;
				case 1 -> item.unit;
				default -> {
					int idx = col - 2;
					yield idx >= 0 && idx < item.values.length
						? item.values[idx]
						: null;
				}
			};
		}

		@Override
		public Color getForeground(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			return switch (col) {
				case 0 -> item.hasOpenEpdCode() ? null : Colors.red();
				case 1 -> item.hasOpenEpdUnit() ? null : Colors.red();
				default -> null;
			};
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}
	}

	private record Item(String indicator, String unit, String[] values) {

		static List<Item> allOf(List<EpdIndicatorResult> results, String[] mods) {
			var items = new ArrayList<Item>();
			for (var r : results) {
				var unit = r.values().stream()
					.map(v -> v.value() != null ? v.value().unit() : null)
					.filter(Objects::nonNull)
					.findAny()
					.orElse(null);
				var values = new String[mods.length];
				for (int i = 0; i < mods.length; i++) {
					var mod = mods[i];
					var d = r.values().stream()
						.filter(v -> Objects.equals(mod, v.scope()))
						.map(EpdScopeValue::value)
						.filter(Objects::nonNull)
						.mapToDouble(EpdMeasurement::mean)
						.findAny();
					values[i] = d.isPresent()
						? Double.toString(d.getAsDouble())
						: " - ";
				}
				items.add(new Item(r.indicator(), unit, values));
			}
			return items;
		}

		boolean hasOpenEpdCode() {
			if (indicator == null)
				return false;
			return switch (indicator) {
				case "gwp", "odp", "pocp", "ap", "ep",
					"gwp-fossil", "gwp-biogenic", "gwp-luluc", "gwp-nonCO2",
					"ep-marine", "ep-fresh", "ep-terr" -> true;
				default -> false;
			};
		}

		boolean hasOpenEpdUnit() {
			if (!hasOpenEpdCode())
				return false;
			var expected = switch (indicator) {
				case "gwp", "gwp-fossil", "gwp-biogenic", "gwp-luluc", "gwp-nonCO2" ->
					"kgCO2e";
				case "odp" -> "kgCFC11e";
				case "pocp" -> "kgO3e";
				case "ap" -> "kgSO2e";
				case "ep", "ep-marine" -> "kgNe";
				case "ep-fresh" -> "kgPO4e";
				case "ep-terr" -> "molNe";
				default -> null;
			};
			return Strings.nullOrEqual(expected, unit);
		}

		static boolean hasNonEpdItems(List<Item> items) {
			for (var item : items) {
				if (!item.hasOpenEpdUnit())
					return true;
			}
			return false;
		}
	}

}
