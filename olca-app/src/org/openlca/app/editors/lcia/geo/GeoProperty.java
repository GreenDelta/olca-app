package org.openlca.app.editors.lcia.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.util.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Describes a numeric property of a set of geometric features. Such properties
 * can be then used to calculate regionalized characterization factors.
 */
class GeoProperty {

	/**
	 * The name of the parameter in the GeoJSON file.
	 */
	String name;

	/**
	 * The identifier of the parameter for usage in formulas
	 */
	String identifier;

	/**
	 * The minimum value of the parameter in the features of the GeoJSON file.
	 */
	double min;

	/**
	 * The maximum value of the parameter in the features of the GeoJSON file.
	 */
	double max;

	/**
	 * The default value that should be taken in the calculation of regionalized
	 * factors when no intersecting geometry was found.
	 */
	double defaultValue;

	/**
	 * Defines how multiple values of this parameter in different geographic
	 * features are aggregated.
	 */
	GeoAggregation aggregation;

	static GeoProperty fromJson(JsonObject obj) {
		return obj == null
				? null
				: new Gson().fromJson(obj, GeoProperty.class);
	}

	JsonObject toJson() {
		return new Gson()
				.toJsonTree(this)
				.getAsJsonObject();
	}

	/**
	 * Collects the parameters / attributes from the given features.
	 */
	static List<GeoProperty> collectFrom(FeatureCollection coll) {
		if (coll == null || coll.isEmpty())
			return Collections.emptyList();

		Map<String, GeoProperty> map = new HashMap<>();
		Map<String, Double> sums = new HashMap<>();
		Map<String, Integer> counts = new HashMap<>();

		for (Feature f : coll.features) {
			if (f.properties == null)
				continue;
			for (Map.Entry<String, Object> e : f.properties.entrySet()) {
				if (e.getKey() == null)
					continue;
				Object obj = e.getValue();
				if (!(obj instanceof Number))
					continue;

				String id = e.getKey().replaceAll("[\\W]", "_").toLowerCase();
				double val = ((Number) obj).doubleValue();

				// update the values for computing the average
				sums.compute(id, (key, oldVal) -> oldVal == null
						? val
						: oldVal + val);
				counts.compute(id, (key, oldVal) -> oldVal == null
						? 1
						: oldVal + 1);

				GeoProperty param = map.get(id);
				if (param != null) {
					param.min = Math.min(param.min, val);
					param.max = Math.max(param.max, val);
					continue;
				}
				param = new GeoProperty();
				param.name = e.getKey();
				param.identifier = id;
				param.min = val;
				param.max = val;
				map.put(id, param);
			}
		}

		// set the average values as default values
		var params = new ArrayList<GeoProperty>();
		for (GeoProperty param : map.values()) {
			Double sum = sums.get(param.identifier);
			Integer count = counts.get(param.identifier);
			param.defaultValue = sum != null && count != null
					? sum / count
					: 1;
			params.add(param);
		}
		params.sort((p1, p2) -> Strings.compare(p1.name, p2.name));
		return params;
	}
}
