package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.io.CategoryPath;

class GeoFlowSection {

	private final GeoPage page;
	private TableViewer table;

	GeoFlowSection(GeoPage page) {
		this.page = page;
	}

	void drawOn(Composite body, FormToolkit tk) {

		// create the section
		Section section = UI.section(body, tk, "Flow bindings");
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		// create the table
		table = Tables.createViewer(comp,
				M.Flow,
				M.Category,
				M.Formula,
				M.Unit);
		table.setLabelProvider(new Label());
		Tables.bindColumnWidths(
				table, 0.25, 0.25, 0.25, 0.25);
		ModifySupport<GeoFlowBinding> ms = new ModifySupport<>(table);
		// ms.bind(M.Formula, /* TODO: new FormulaCell() */ null);

		// TODO: bind actions
	}

	void update() {
		if (page.setup == null)
			return;
		table.setInput(page.setup.bindings);
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		@Override
	  public Image getColumnImage(Object obj, int col) {

	    if (!(obj instanceof GeoFlowBinding))
	      return null;
	    GeoFlowBinding b = (GeoFlowBinding) obj;
	    if (b.flow == null)
	      return null;

	    switch (col) {
	      case 0:
	        return Images.get(b.flow);
	      case 1:
	        return Images.get(b.flow.category);
	      case 2:
	        return Icon.FORMULA.get();
	      default:
	        return null;
	  }
		}

		@Override
		public String getColumnText(Object obj, int col) {

			if (!(obj instanceof GeoFlowBinding))
				return null;
			GeoFlowBinding b = (GeoFlowBinding) obj;
			if (b.flow == null)
				return null;

			switch (col) {
			case 0:
				return Labels.name(b.flow);
			case 1:
				return CategoryPath.getFull(b.flow.category);
			case 2:
				return b.formula;
			case 3:
				return Labels.name(b.flow.getReferenceUnit());
			default:
				return null;
			}
		}
	}
}
