package org.openlca.app.editors.lcia.geo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Setup {

	/** The full path to the GeoJSON file. */
	final String file;

	/** The parameters of the GeoJSON file. */
	final List<GeoParam> params = new ArrayList<>();

	/**
	 * The elementary flows that are bound to parameters if the GeoJSON file via
	 * parameters. These formulas are then used to calculate the reionalized
	 * characterization factors.
	 */
	final List<GeoFlowBinding> bindings = new ArrayList<>();

	private FeatureCollection features;

	private Setup(String file) {
		this.file = file;
	}

	static Setup create(File geojson) {
		if (geojson == null)
			return null;
		Setup setup = new Setup(
				geojson.getAbsolutePath());
		setup.getFeatures();
		setup.initParams();
		return setup;
	}

	FeatureCollection getFeatures() {
		if (features != null)
			return features;
		if (Strings.nullOrEmpty(file)) {
			features = new FeatureCollection();
			return features;
		}
		try {
			features = GeoJSON.read(new File(file));
			return features;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to read GeoJSON file " + file);
			features = new FeatureCollection();
			return features;
		}
	}

	private void initParams() {
		FeatureCollection coll = getFeatures();
		if (coll == null)
			return;
		params.clear();

		Map<String, GeoParam> map = new HashMap<>();
		for (Feature f : coll.features) {
			if (f.properties == null)
				continue;
			for (Map.Entry<String, Object> e : f.properties.entrySet()) {
				if (e.getKey() == null)
					continue;
				Object obj = e.getValue();
				if (!(obj instanceof Number))
					continue;
				String id = e.getKey().replaceAll("[\\W]", "_")
						.toLowerCase();
				double val = ((Number) obj).doubleValue();
				GeoParam param = map.get(id);
				if (param != null) {
					param.min = Math.min(param.min, val);
					param.max = Math.max(param.max, val);
					continue;
				}
				param = new GeoParam();
				param.name = e.getKey();
				param.identifier = id;
				param.min = val;
				param.max = val;
				map.put(id, param);
			}
		}
		params.addAll(map.values());
		Collections.sort(params,
				(p1, p2) -> Strings.compare(p1.name, p2.name));
	}

}
