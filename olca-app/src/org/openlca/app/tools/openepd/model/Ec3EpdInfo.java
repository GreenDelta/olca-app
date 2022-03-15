package org.openlca.app.tools.openepd.model;

import com.google.gson.JsonElement;
import org.openlca.jsonld.Json;

import java.util.Optional;

/**
 * Contains some meta-data of an EPD on EC3.
 */
public class Ec3EpdInfo {

	public String id;
	public String name;
	public String description;
	public Ec3Category category;
	public String declaredUnit;
	public Ec3OrgInfo manufacturer;

	public static Optional<Ec3EpdInfo> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var info = new Ec3EpdInfo();
		info.id = Json.getString(obj, "id");
		info.name = Json.getString(obj, "name");
		info.description = Json.getString(obj, "description");
		info.category = Ec3Category.fromJson(
			obj.get("category")).orElse(null);
		info.declaredUnit = Json.getString(obj, "declared_unit");
		info.manufacturer = Ec3OrgInfo.fromJson(
			obj.get("manufacturer")).orElse(null);
		return Optional.of(info);
	}

}
