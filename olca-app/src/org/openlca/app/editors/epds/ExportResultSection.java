package org.openlca.app.editors.epds;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.tools.openepd.model.Ec3ImpactResult;
import org.openlca.app.tools.openepd.model.Ec3IndicatorResult;
import org.openlca.app.tools.openepd.model.Ec3Measurement;
import org.openlca.app.tools.openepd.model.Ec3ScopeValue;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

class ExportResultSection {

	private final Ec3ImpactResult result;
	private final String[] mods;

	ExportResultSection(Ec3ImpactResult result) {
		this.result = result;
		var mods = new HashSet<String>();
		result.indicatorResults()
			.forEach(i -> i.values().forEach(s -> mods.add(s.scope())));
		this.mods = mods.stream().sorted().toArray(String[]::new);
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Results: " + result.method());
		var comp = UI.sectionClient(section, tk, 1);
		var columns = new String[mods.length + 2];
		columns[0] = "Indicator";
		columns[1] = "Unit";
		System.arraycopy(mods, 0, columns, 2, mods.length);
		var table = Tables.createViewer(comp, columns);
		double[] colWidths = new double[2 + mods.length];
		Arrays.fill(colWidths, 1.0 / (2.0 + mods.length));
		Tables.bindColumnWidths(table, colWidths);
		table.setLabelProvider(new TableLabel());
		table.setInput(result.indicatorResults());
	}

	private class TableLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Ec3IndicatorResult r))
				return null;
			return switch (col) {
				case 0 -> r.indicator();
				case 1 -> r.values().stream()
					.map(v -> v.value() != null ? v.value().unit() : null)
					.filter(Objects::nonNull)
					.findAny()
					.orElse(null);
				default -> {
					int modIdx = col - 2;
					if (modIdx < 0 || modIdx >= mods.length)
						yield null;
					var mod = mods[modIdx];
					var d = r.values().stream()
						.filter(v -> Objects.equals(mod, v.scope()))
						.map(Ec3ScopeValue::value)
						.filter(Objects::nonNull)
						.mapToDouble(Ec3Measurement::mean)
						.findAny();
					yield d.isPresent()
						? Double.toString(d.getAsDouble())
						: " - ";
				}
			};
		}
	}
}
