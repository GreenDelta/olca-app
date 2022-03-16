package org.openlca.app.tools.openepd.model;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

/**
 * A measurement value.
 *
 * @param mean (expected) mean value of the measurement
 * @param unit the measurement unit
 * @param rsd  the relative standard deviation, i.e. standard_deviation/mean
 * @param dist statistical distribution of the measurement error
 */
public record EpdMeasurement(
	double mean,
	String unit,
	Double rsd,
	String dist
) implements Jsonable {

	public static EpdMeasurement of(double amount, String unit) {
		return new EpdMeasurement(amount, unit, null, null);
	}

	public static Optional<EpdMeasurement> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var m = new EpdMeasurement(
			Json.getDouble(obj, "mean", 0),
			Json.getString(obj, "unit"),
			Json.getDouble(obj, "rsd").orElse(null),
			Json.getString(obj, "dist")
		);
		return Optional.of(m);
	}

	@Override
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

