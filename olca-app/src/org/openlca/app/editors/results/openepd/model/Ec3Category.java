package org.openlca.app.editors.results.openepd.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

public class Ec3Category {

	public String id;
	public String name;
	public String description;
	public final List<String> parents = new ArrayList<>();
	public final List<Ec3Category> subCategories = new ArrayList<>();

	public static Optional<Ec3Category> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var category = new Ec3Category();
		category.id = Json.getString(obj, "id");
		category.name = Json.getString(obj, "display_name");
		category.description = Json.getString(obj, "description");

		Json.stream(Json.getArray(obj, "parents"))
			.filter(JsonElement::isJsonPrimitive)
			.map(JsonElement::getAsString)
			.forEach(category.parents::add);

		Json.stream(Json.getArray(obj, "subcategories"))
			.filter(JsonElement::isJsonObject)
			.map(Ec3Category::fromJson)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(category.subCategories::add);

		return Optional.of(category);
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		Json.put(obj, "id", id);
		Json.put(obj, "name", name);
		Json.put(obj, "description", description);

		if (!parents.isEmpty()) {
			var parentsArray = new JsonArray();
			for (var p : parents) {
				parentsArray.add(p);
			}
			obj.add("parents", parentsArray);
		}

		if (!subCategories.isEmpty()) {
			var catArray = new JsonArray();
			for (var c : subCategories) {
				catArray.add(c.toJson());
			}
			obj.add("subcategories", catArray);
		}
		return obj;
	}
}
