package org.openlca.app.editors.locations;

import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.mapview.LayerConfig;
import org.openlca.app.components.mapview.MapView;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;
import org.openlca.geo.geojson.MsgPack;
import org.openlca.util.BinUtils;
import org.python.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MapPage extends ModelPage<Location> {

	private MapView map;
	private FeatureCollection feature;
	private LayerConfig layer;

	MapPage(LocationEditor editor) {
		super(editor, "MapPage", "GeoJSON");
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		Section section = UI.section(body, tk, "Map");
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		comp.setLayout(new FillLayout());
		UI.gridData(comp, true, true);

		map = new MapView(comp);
		map.addBaseLayers();
		feature = unpack(getModel());
		if (feature != null) {
			updateMap();
		}

		Action edit = Actions.onEdit(() -> {
			new GeoJSONDialog().open();
		});
		Actions.bind(section, edit);

	}

	private void updateMap() {
		if (map == null)
			return;
		if (layer != null) {
			map.removeLayer(layer);
		}
		if (feature == null) {
			map.update();
			return;
		}
		layer = map.addLayer(feature)
				.fillColor(Colors.get(173, 20, 87, 100))
				.borderColor(Colors.get(173, 20, 87, 100))
				.center();
		map.update();
	}

	private FeatureCollection unpack(Location loc) {
		if (loc == null || loc.geodata == null)
			return null;
		try {
			byte[] data = BinUtils.gunzip(loc.geodata);
			return MsgPack.unpack(data);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to load geodata from " + loc, e);
			return null;
		}
	}

	private byte[] pack(FeatureCollection coll) {
		if (coll == null)
			return null;
		try {
			byte[] data = MsgPack.pack(coll);
			return BinUtils.gzip(data);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to unpack geo data from " + getModel(), e);
			return null;
		}
	}

	private class GeoJSONDialog extends Dialog {

		private Text text;

		public GeoJSONDialog() {
			super(UI.shell());
		}

		@Override
		protected Control createDialogArea(Composite root) {
			getShell().setText("Enter GeoJSON");
			Composite area = (Composite) super.createDialogArea(root);
			UI.gridLayout(area, 1);
			new Label(area, SWT.NONE).setText(
					"See e.g. http://geojson.io for examples");
			text = new Text(area, SWT.MULTI | SWT.BORDER);
			UI.gridData(text, true, true);
			text.setText(getInitialJson());
			return area;
		}

		private String getInitialJson() {
			if (feature == null)
				return "";
			if (feature.features.isEmpty())
				return "";
			Feature f = feature.features.get(0);
			if (f.geometry == null)
				return "";
			StringWriter w = new StringWriter();
			GeoJSON.write(f.geometry, w);
			return w.toString();
		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 400);
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected void okPressed() {
			String json = text.getText();
			try {
				if (Strings.isNullOrEmpty(json)) {
					if (feature == null)
						return;
					feature = null;
					getModel().geodata = null;
				} else {
					feature = GeoJSON.read(new StringReader(json));
					getModel().geodata = pack(feature);
				}
			} catch (Exception e) {
				MsgBox.error("Failed to parse GeoJSON",
						"Please check the format of the given GeoJSON string.");
			} finally {
				getEditor().setDirty(true);
				updateMap();
				super.okPressed();
			}
		}
	}
}
