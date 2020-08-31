package org.openlca.app.editors.locations;

import java.io.StringReader;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.mapview.LayerConfig;
import org.openlca.app.components.mapview.MapView;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;

import com.google.gson.GsonBuilder;
import org.openlca.util.Strings;

class MapSection {

	private final LocationEditor editor;

	private MapView map;
	private FeatureCollection feature;
	private LayerConfig layer;

	MapSection(LocationEditor editor) {
		this.editor = editor;
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Geographic data");
		UI.gridData(section, true, true).minimumHeight = 250;
		var comp = UI.sectionClient(section, tk);
		comp.setLayout(new FillLayout());
		UI.gridData(comp, true, true);

		// render the initial feature
		map = new MapView(comp);
		map.addBaseLayers();
		feature = GeoJSON.unpack(location().geodata);
		if (feature != null) {
			updateMap();
		}

		// bind actions
		if (!editor.isEditable())
			return;
		var edit = Actions.onEdit(() -> {
			new GeoJSONDialog().open();
		});
		Actions.bind(section, edit);
	}

	private Location location() {
		return editor.getModel();
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


	private class GeoJSONDialog extends Dialog {

		private StyledText text;

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
			text = new StyledText(area, SWT.MULTI | SWT.BORDER
					| SWT.V_SCROLL | SWT.H_SCROLL);
			text.setAlwaysShowScrollBars(false);
			UI.gridData(text, true, true);
			text.setText(getJsonText());
			return area;
		}

		private String getJsonText() {
			if (feature == null)
				return "";
			if (feature.features.isEmpty())
				return "";
			Feature f = feature.features.get(0);
			if (f.geometry == null)
				return "";
			return new GsonBuilder()
					.setPrettyPrinting()
					.create()
					.toJson(f.toJson());
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
			var json = text.getText();
			try {
				if (Strings.nullOrEmpty(json)) {
					if (feature == null)
						return;
					feature = null;
					location().geodata = null;
				} else {
					feature = GeoJSON.read(new StringReader(json));
					location().geodata = GeoJSON.pack(feature);
				}
			} catch (Exception e) {
				MsgBox.error("Failed to parse GeoJSON",
						"Please check the format of the given GeoJSON string.");
			} finally {
				editor.setDirty(true);
				updateMap();
				super.okPressed();
			}
		}
	}

}
