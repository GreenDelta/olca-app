package org.openlca.app.tools.openepd.model;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

public class Ec3Certifier implements Jsonable {

	public String name;
	public String email;
	public EpdOrg org;

	public static Optional<Ec3Certifier> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var certifier = new Ec3Certifier();
		certifier.name = Json.getString(obj, "name");
		certifier.email = Json.getString(obj, "email");
		certifier.org = EpdOrg.fromJson(obj.get("org")).orElse(null);
		return Optional.of(certifier);
	}

	@Override
	public JsonObject toJson() {
		var obj = new JsonObject();
		Json.put(obj, "name", name);
		Json.put(obj, "email", email);
		Util.put(obj,"org", org);
		return obj;
	}

}
