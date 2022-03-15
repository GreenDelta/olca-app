package org.openlca.app.tools.openepd.model;

import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

import java.time.LocalDate;

class Util {

	static LocalDate getDate(JsonObject obj, String field) {
		var s = Json.getString(obj, field);
		if (s == null)
			return null;
		try {
			return LocalDate.parse(s);
		} catch (Exception e) {
			return null;
		}
	}

	static EpdQuantity getQuantity(JsonObject obj, String field) {
		if (obj == null || field == null)
			return null;
		return EpdQuantity.fromJson(obj.get(field))
			.orElse(null);
	}
}
