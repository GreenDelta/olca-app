package org.openlca.app.tools.openepd.model;

import com.google.gson.JsonElement;
import org.openlca.app.collaboration.util.Json;

import java.util.Optional;

public class Ec3OrgInfo {

	public String id;
	public String name;

	public static Optional<Ec3OrgInfo> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var info = new Ec3OrgInfo();
		info.id = Json.getString(obj, "id");
		info.name = Json.getString(obj, "name");
		return Optional.of(info);
	}

}
