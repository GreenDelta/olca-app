package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;

class GeoParamSection {

	private final GeoPage page;
	private TableViewer table;

	GeoParamSection(GeoPage page) {
		this.page = page;
	}

	void drawOn(Composite body, FormToolkit tk) {
	    Composite comp = UI.formSection(
	      body, tk, "GeoJSON Parameters");
	    UI.gridLayout(comp, 1);
		table = Tables.createViewer(comp,
				"Parameter", "Identifier", "Range",
				"Aggregation type");
		table.setLabelProvider(new Label());
		update();
	  }

	void update() {
		if (page.setup == null)
			return;
		table.setInput(page.setup.params);
	}

	private class Label extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 1) {
				return Icon.FORMULA.get();
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof GeoParam))
				return null;
			GeoParam p = (GeoParam) obj;
			switch (col) {
			case 0:
				return p.name;
			case 1:
				return p.identifier;
			case 2:
				return "[" + Numbers.format(p.min)
						+ ", " + Numbers.format(p.max) + "]";
			case 3:
				return "Weighted average"; // TODO
			default:
				return null;
			}
		}
	}
}
