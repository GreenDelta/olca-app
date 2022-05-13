package org.openlca.app.tools.openepd.export;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;

import java.util.Arrays;

record ResultSection(ResultModel model) {

	void render(Composite body, FormToolkit tk) {
		var title = model.method != null
			? "Results: " + Labels.name(model.method)
			: "Results";
		var section = UI.section(body, tk, title);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);

		// TODO: method column

		var columns = new String[5 + model.scopes.size()];
		columns[0] = "Indicator";
		columns[1] = "Unit";
		columns[2] = "openEPD Indicator";
		columns[3] = "openEPD Unit";
		columns[4] = "Factor";
		for (int i = 0; i < model.scopes.size(); i++) {
			columns[i + 5] = model.scopes.get(i);
		}
		var table = Tables.createViewer(comp, columns);
		table.setLabelProvider(new TableLabel());
		var widths = new double[columns.length];
		widths[0] = 0.2;
		widths[1] = 0.1;
		widths[2] = 0.1;
		widths[3] = 0.1;
		widths[4] = 0.1;
		Arrays.fill(widths, 5, columns.length, 
				.4 / (model.scopes.size()));
		Tables.bindColumnWidths(table, widths);
		for (int i = 4; i < columns.length; i++) {
			table.getTable()
				.getColumn(i)
				.setAlignment(SWT.CENTER);
		}
		table.setInput(model.rows);
	}

	private class TableLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0
				? Images.get(ModelType.IMPACT_CATEGORY)
				: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ResultRow row))
				return null;
			return switch (col) {
				case 0 -> Labels.name(row.indicator);
				case 1 -> row.indicator.referenceUnit;
				case 2 -> row.epdIndicator != null
					? row.epdIndicator.code()
					: " - ";
				case 3 -> row.epdIndicator != null
					? row.epdIndicator.unit()
					: " - ";
				case 4 -> Double.toString(row.factor);
				default -> {
					int idx = col - 5;
					if (idx < 0 || idx >= model.scopes.size())
						yield " - ";
					var scope = model.scopes.get(idx);
					var value = row.values.get(scope);
					yield value == null
						? " - "
						: Numbers.format(value * row.factor);
				}
			};
		}
	}

}
