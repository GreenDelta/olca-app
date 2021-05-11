package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.components.mapview.LayerConfig;
import org.openlca.app.components.mapview.MapView;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.geo.calc.Bounds;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;
import org.openlca.geo.geojson.Geometry;
import org.openlca.util.Pair;

class ResultMap {

	private MapView map;
	private FeatureCollection coll;
	private LayerConfig layer;

	/**
	 * Caches the sizes of the location bounds. We want to render smaller geometries
	 * on top of larger geometries and to do this, we sort them by the size of their
	 * bounds.
	 */
	private final Map<Location, Double> bsize = new HashMap<>();

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
		Actions.bind(section, Actions.create(
				M.Export, Icon.EXPORT.descriptor(), m::export));
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
		List<Pair<Location, Feature>> pairs = new ArrayList<>();
		for (Contribution<Location> c : contributions) {
			Location loc = c.item;
			if (loc == null || loc.geodata == null)
				continue;
			FeatureCollection fc = GeoJSON.unpack(loc.geodata);
			if (fc == null || fc.features.isEmpty())
				continue;
			Geometry g = fc.features.get(0).geometry;
			if (g == null)
				continue;
			Feature feature = new Feature();
			feature.geometry = g;
			feature.properties = new HashMap<>();
			feature.properties.put("result", c.amount);
			addMetaData(loc, feature, selection);
			pairs.add(Pair.of(loc, feature));
		}

		if (pairs.isEmpty())
			return;
		pairs.stream().sorted((p1, p2) -> {
			return Double.compare(bsize(p2), bsize(p1));
		}).forEach(p -> coll.features.add(p.second));

		layer = map.addLayer(coll)
				.fillScale("result")
				.center();
		map.update();
	}

	private void addMetaData(Location loc, Feature f, Object selection) {
		f.properties.put("location_code", loc.code);
		f.properties.put("location", loc.name);
		f.properties.put("location_id", loc.refId);
		if (selection instanceof FlowDescriptor) {
			FlowDescriptor flow = (FlowDescriptor) selection;
			f.properties.put("flow_id", flow.refId);
			f.properties.put("flow", Labels.name(flow));
			f.properties.put("flow_category", Labels.category(flow));
			f.properties.put("unit", Labels.refUnit(flow));
			return;
		}

		if (selection instanceof ImpactDescriptor) {
			var imp = (ImpactDescriptor) selection;
			f.properties.put("impact_id", imp.refId);
			f.properties.put("impact_name", imp.name);
			f.properties.put("unit", imp.referenceUnit);
			return;
		}

		if (selection instanceof CostResultDescriptor) {
			CostResultDescriptor c = (CostResultDescriptor) selection;
			f.properties.put("cost_type", c.forAddedValue
					? "added value"
					: "net costs");
			f.properties.put("unit", Labels.getReferenceCurrencyCode());
		}
	}

	private double bsize(Pair<Location, Feature> pair) {
		return bsize.computeIfAbsent(pair.first, loc -> {
			Feature f = pair.second;
			Bounds bounds = Bounds.of(f);
			return Math.abs(bounds.maxX - bounds.minX)
					* Math.abs(bounds.maxY - bounds.minY);
		});
	}

	private void export() {
		if (coll == null) {
			MsgBox.info("No data",
					"The map does not contain result data.");
			return;
		}
		var file = FileChooser.forSavingFile(M.Export, "result.geojson");
		if (file == null)
			return;
		try {
			GeoJSON.write(coll, file);
			Popup.info("Export done",
					"Result map was written to " + file.getName());
		} catch (Exception e) {
			ErrorReporter.on("Failed to export result map to: " + file, e);
		}
	}
}
