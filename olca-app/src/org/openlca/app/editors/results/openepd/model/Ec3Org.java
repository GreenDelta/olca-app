package org.openlca.app.editors.results.openepd.model;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

public class Ec3Org {

	public String id;
	public String name;
	public String address;
	public String website;
	public String country;
	public double latitude;
	public double longitude;

	public static Optional<Ec3Org> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var org = new Ec3Org();
		org.id = Json.getString(obj, "id");
		org.name = Json.getString(obj, "name");
		org.address = Json.getString(obj, "address");
		org.website = Json.getString(obj, "website");
		org.country = Json.getString(obj, "country");
		org.latitude = Json.getDouble(obj, "latitude", 0);
		org.longitude = Json.getDouble(obj, "longitude", 0);
		return Optional.of(org);
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		Json.put(obj, "id", id);
		Json.put(obj, "name", name);
		Json.put(obj, "website", website);
		Json.put(obj, "country", country);
		if (!(latitude == 0 && longitude == 0)) {
			obj.addProperty("latitude", latitude);
			obj.addProperty("longitude", longitude);
		}
		return obj;
	}
}
