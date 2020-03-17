package org.openlca.app.editors.lcia.geo;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class GeoParam {

	/** The name of the parameter in the GeoJSON file. */
	String name;

	/** The identifier of the parameter for usage in formulas */
	String identifier;

	/** The minimum value of the parameter in the features of the GeoJSON file. */
	double min;

	/** The maximum value of the parameter in the features of the GeoJSON file. */
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
	GeoAggType aggType;

	static GeoParam fromJson(JsonObject obj) {
		if (obj == null)
			return null;
		return new Gson().fromJson(obj, GeoParam.class);
	}

	JsonObject toJson() {
		JsonElement elem = new Gson().toJsonTree(this);
		return elem.getAsJsonObject();
	}

}
