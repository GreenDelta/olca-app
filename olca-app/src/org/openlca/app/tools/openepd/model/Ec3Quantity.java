package org.openlca.app.tools.openepd.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

import java.util.Optional;

public record Ec3Quantity(double amount, String unit) {

	public static Ec3Quantity of(double amount, String unit) {
		return new Ec3Quantity(amount, unit);
	}

	public static Optional<Ec3Quantity> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var unit = Json.getString(obj, "unit");
		var qty = new Ec3Quantity(
			Json.getDouble(obj, "qty", 0),
			unit != null ? unit : ""
		);
		return Optional.of(qty);
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		obj.addProperty("qty", amount);
		obj.addProperty("unit", unit);
		return obj;
	}

	@Override
	public String toString() {
		return amount + " " + unit;
	}
}
