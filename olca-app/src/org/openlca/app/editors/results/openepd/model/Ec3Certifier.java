package org.openlca.app.editors.results.openepd.model;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

public class Ec3Certifier {

	public String name;
	public String email;
	public Ec3Org org;

	public static Optional<Ec3Certifier> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var certifier = new Ec3Certifier();
		certifier.name = Json.getString(obj, "name");
		certifier.email = Json.getString(obj, "email");
		certifier.org = Ec3Org.fromJson(obj.get("org")).orElse(null);
		return Optional.of(certifier);
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		Json.put(obj, "name", name);
		Json.put(obj, "email", email);
		if (org != null) {
			obj.add("org", org.toJson());
		}
		return obj;
	}

}
