package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.mapview.MapView;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.geo.geojson.FeatureCollection;

class GeoParamSection {

	private final GeoPage page;
	private TableViewer table;

	GeoParamSection(GeoPage page) {
		this.page = page;
	}

	void drawOn(Composite body, FormToolkit tk) {

		// create the section
		Section section = UI.section(body, tk, "GeoJSON Parameters");
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		// create the table
		table = Tables.createViewer(comp,
				"Parameter", "Identifier", "Range",
				"Aggregation type");
		table.setLabelProvider(new Label());
		Tables.bindColumnWidths(
				table, 0.25, 0.25, 0.25, 0.25);

		// bind the "open map" action
		Action openMap = Actions.create(
				"Open map", Icon.MAP.descriptor(), () -> {
					MapDialog dialog = new MapDialog(page.setup);
					dialog.open();
				});
		Actions.bind(table, openMap);
		Actions.bind(section, openMap);

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

	private class MapDialog extends Dialog {

		private final Setup setup;

		public MapDialog(Setup setup) {
			super(UI.shell());
			this.setup = setup;
		}

		@Override
		protected Control createDialogArea(Composite root) {

			Composite area = (Composite) super.createDialogArea(root);
			area.setLayout(new FillLayout());
			if (setup == null || setup.file == null)
				return area;
			getShell().setText(setup.file);

			FeatureCollection coll = setup.getFeatures();
			if (coll == null)
				return area;
			GeoParam gp = Viewers.getFirstSelected(table);
			String param = gp != null ? gp.name : null;

			MapView map = new MapView(area);
			if (param == null) {
				map.addLayer(coll).center();
			} else {
				map.addLayer(coll)
						.fillScale(param)
						.center();
			}
			return area;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(500, 500);
		}

		@Override
		protected boolean isResizable() {
			return true;
		}
	}

}
