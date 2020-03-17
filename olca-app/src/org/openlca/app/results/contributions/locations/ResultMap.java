package org.openlca.app.results.contributions.locations;

import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.mapview.LayerConfig;
import org.openlca.app.components.mapview.MapView;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.core.results.Contribution;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.Geometry;
import org.openlca.geo.geojson.MsgPack;

class ResultMap {

	private MapView map;
	private FeatureCollection coll;
	private LayerConfig layer;

	private ResultMap() {
	}

	static ResultMap on(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, M.Map);
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		comp.setLayout(new FillLayout());
		UI.gridData(comp, true, true);
		ResultMap m = new ResultMap();
		m.map = new MapView(comp);
		m.map.addBaseLayers();
		return m;
	}

	void update(Object selection, List<Contribution<Location>> contributions) {
		if (map == null)
			return;
		if (layer != null) {
			map.removeLayer(layer);
		}
		if (contributions == null || contributions.isEmpty()) {
			coll = null;
			map.update();
			return;
		}

		coll = new FeatureCollection();
		for (Contribution<Location> c : contributions) {
			Location loc = c.item;
			if (loc == null || loc.geodata == null)
				continue;
			FeatureCollection fc = MsgPack.unpackgz(loc.geodata);
			if (fc == null || fc.features.isEmpty())
				continue;
			Geometry g = fc.features.get(0).geometry;
			if (g == null)
				continue;
			Feature feature = new Feature();
			feature.geometry = g;
			feature.properties = new HashMap<String, Object>();
			feature.properties.put("result", c.amount);
			// TODO: add some meta data about the selection
			coll.features.add(feature);
		}

		if (coll.features.isEmpty())
			return;

		layer = map.addLayer(coll)
				.fillScale("result")
				.center();
		map.update();
	}

}
