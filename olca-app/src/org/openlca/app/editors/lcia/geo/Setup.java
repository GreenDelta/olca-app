package org.openlca.app.editors.lcia.geo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

	static Setup read(File geojson) {
		if (geojson == null)
			return null;
		var setup = new Setup(geojson.getAbsolutePath());
		setup.getFeatures();
		var params = GeoParam.collectFrom(setup.features);
		setup.params.addAll(params);
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
}
