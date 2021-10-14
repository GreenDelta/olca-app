package org.openlca.app.editors.results.openepd.model;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

public class Ec3Measurement {

	/**
	 * Mean (expected) value of the measurement
	 */
	public double mean;

	/**
	 * The measurement unit.
	 */
	public String unit;

	/**
	 * The relative standard deviation, i.e. standard_deviation/mean.
	 */
	public Double rsd;

	/**
	 * Statistical distribution of the measurement error.
	 */
	public String dist;

	public static Ec3Measurement of(double amount, String unit) {
		var m = new Ec3Measurement();
		m.mean = amount;
		m.unit = unit;
		return m;
	}

	public static Optional<Ec3Measurement> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var m = new Ec3Measurement();
		m.mean = Json.getDouble(obj, "mean", 0);
		m.unit = Json.getString(obj, "unit");
		m.rsd = Json.getDouble(obj, "rsd").orElse(null);
		m.dist = Json.getString(obj, "dist");
		return Optional.of(m);
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		obj.addProperty("mean", mean);
		Json.put(obj, "unit", unit);
		if (rsd != null) {
			obj.addProperty("rsd", rsd);
		}
		Json.put(obj, "dist", dist);
		return obj;
	}
}

