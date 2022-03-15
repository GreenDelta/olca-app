package org.openlca.app.tools.openepd.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

/**
 * The Ec3 internal EPD format.
 */
public class Ec3Epd {

	public String id;
	public String name;
	public String description;
	public Ec3Category category;
	public String categoryId;

	public String declaredUnit;
	public String massPerDeclaredUnit;
	public String gwp;
	public String gwpPerKg;

	public Ec3Certifier reviewer;
	public Ec3Certifier developer;
	public Ec3Certifier verifier;
	public EpdOrg manufacturer;

	public String docUrl;

	public boolean isDraft;
	public boolean isPrivate;
	public LocalDate dateOfIssue;
	public LocalDate dateValidityEnds;

	public final List<Ec3ImpactResult> impactResults = new ArrayList<>();

	public static Optional<Ec3Epd> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var epd = new Ec3Epd();
		epd.id = Json.getString(obj, "id");
		epd.name = Json.getString(obj, "name");
		epd.description = Json.getString(obj, "description");
		epd.category = Ec3Category.fromJson(
			obj.get("category")).orElse(null);
		epd.categoryId = epd.category != null
			? epd.category.id
			: Json.getString(obj, "category_id");

		epd.declaredUnit = Json.getString(obj, "declared_unit");
		epd.massPerDeclaredUnit = Json.getString(obj, "mass_per_declared_unit");
		epd.gwp = Json.getString(obj, "gwp");
		epd.gwpPerKg = Json.getString(obj, "gwp_per_kg");

		epd.reviewer = Ec3Certifier.fromJson(obj.get("reviewer")).orElse(null);
		epd.developer = Ec3Certifier.fromJson(obj.get("developer")).orElse(null);
		epd.verifier = Ec3Certifier.fromJson(obj.get("verifier")).orElse(null);
		epd.manufacturer = EpdOrg.fromJson(obj.get("manufacturer")).orElse(null);

		epd.docUrl = Json.getString(obj, "doc");
		epd.isDraft = Json.getBool(obj, "draft", false);
		epd.isPrivate = Json.getBool(obj, "private", false);
		epd.dateOfIssue = Util.getDate(obj, "date_of_issue");
		epd.dateValidityEnds = Util.getDate(obj, "date_validity_ends");

		// impacts
		var impactJson = Json.getObject(obj, "impacts");
		if (impactJson != null) {
			var impactResults = Ec3ImpactResult.fromJson(impactJson);
			epd.impactResults.addAll(impactResults);
		}

		return Optional.of(epd);
	}

	public JsonObject toJson() {
		var obj = new JsonObject();

		Json.put(obj, "id", id);
		Json.put(obj, "name", name);
		Json.put(obj, "description", description);

		Json.put(obj, "declared_unit", declaredUnit);
		Json.put(obj, "mass_per_declared_unit", massPerDeclaredUnit);
		Json.put(obj, "gwp", gwp);
		Json.put(obj, "gwp_per_kg", gwpPerKg);
		if (category != null) {
			obj.add("category", category.toJson());
		}
		Json.put(obj, "category_id", categoryId);

		Json.put(obj, "doc", docUrl);
		obj.addProperty("draft", isDraft);
		obj.addProperty("private", isPrivate);

		if (dateOfIssue != null) {
			obj.addProperty("date_of_issue", dateOfIssue.toString());
		}
		if (dateValidityEnds != null) {
			obj.addProperty("date_validity_ends", dateValidityEnds.toString());
		}

		if (reviewer != null) {
			obj.add("reviewer", reviewer.toJson());
		}
		if (developer != null) {
			obj.add("developer", developer.toJson());
		}
		if (verifier != null) {
			obj.add("verifier", verifier.toJson());
		}

		// impact results
		var impactJson = Ec3ImpactResult.toJson(impactResults);
		if (impactJson.size() > 0) {
			obj.add("impacts", impactJson);
		}

		return obj;
	}

}
