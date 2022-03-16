package org.openlca.app.tools.openepd.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;
import org.openlca.util.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An openEPD document.
 */
public class EpdDoc {

	public String id;
	public String ref;
	public String language;
	public boolean isPrivate = true;
	public String declarationUrl;
	public String lcaDiscussion;

	public LocalDate dateOfIssue;
	public LocalDate dateValidityEnds;
	public EpdQuantity declaredUnit;
	public EpdQuantity kgPerDeclaredUnit;

	public String productName;
	public String productDescription;
	public final List<Pair<String, String>> productClasses = new ArrayList<>();
	public EpdOrg manufacturer;
	public EpdOrg verifier;
	public EpdOrg programOperator;
	public EpdPcr pcr;

	public final List<EpdImpactResult> impactResults = new ArrayList<>();

	public static Optional<EpdDoc> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();

		var obj = elem.getAsJsonObject();
		var epd = new EpdDoc();

		epd.id = Json.getString(obj, "id");
		epd.ref = Json.getString(obj, "ref");
		epd.language = Json.getString(obj, "language");
		epd.isPrivate = Json.getBool(obj, "private", false);
		epd.declarationUrl = Json.getString(obj, "declaration_url");
		epd.lcaDiscussion = Json.getString(obj, "lca_discussion");

		epd.dateOfIssue = Util.getDate(obj, "date_of_issue");
		epd.dateValidityEnds = Util.getDate(obj, "valid_until");
		epd.declaredUnit = Util.getQuantity(obj, "declared_unit");
		epd.kgPerDeclaredUnit = Util.getQuantity(obj, "kg_per_declared_unit");

		epd.productName = Json.getString(obj, "product_name");
		epd.productDescription = Json.getString(obj, "product_description");

		epd.manufacturer = EpdOrg.fromJson(
			obj.get("manufacturer")).orElse(null);
		epd.verifier = EpdOrg.fromJson(
			obj.get("third_party_verifier")).orElse(null);
		epd.programOperator = EpdOrg.fromJson(
			obj.get("program_operator")).orElse(null);
		epd.pcr = EpdPcr.fromJson(obj.get("pcr")).orElse(null);

		// classes / categories
		var classes = Json.getObject(obj, "product_classes");
		if (classes != null) {
			for (var e : classes.entrySet()) {
				var classification = e.getKey();
				var classVal = e.getValue();
				if (!classVal.isJsonPrimitive())
					continue;
				epd.productClasses.add(
					Pair.of(classification, classVal.getAsString()));
			}
		}

		// impacts
		var impactJson = Json.getObject(obj, "impacts");
		if (impactJson != null) {
			var impactResults = EpdImpactResult.fromJson(impactJson);
			epd.impactResults.addAll(impactResults);
		}

		return Optional.of(epd);
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		Json.put(obj, "doctype", "OpenEPD");
		Json.put(obj, "id", id);
		Json.put(obj, "language", language != null ? language : "en");
		Json.put(obj, "private", isPrivate);
		Json.put(obj, "declaration_url", declarationUrl);
		Json.put(obj, "lca_discussion", lcaDiscussion);

		Util.put(obj, "date_of_issue", dateOfIssue);
		Util.put(obj, "valid_until", dateValidityEnds);
		Util.put(obj, "declared_unit", declaredUnit);
		Util.put(obj, "kg_per_declared_unit", kgPerDeclaredUnit);

		Json.put(obj, "product_name", productName);
		Json.put(obj, "product_description", productDescription);

		Util.put(obj, "manufacturer", manufacturer);
		Util.put(obj, "third_party_verifier", verifier);
		Util.put(obj, "program_operator", programOperator);
		Util.put(obj, "pcr", pcr);

		// product classes / categories
		if (!productClasses.isEmpty()) {
			var classes = new JsonObject();
			for (var productClass : productClasses) {
				Json.put(classes, productClass.first, productClass.second);
			}
			Json.put(obj, "product_classes", classes);
		}

		// impact results
		var impactJson = EpdImpactResult.toJson(impactResults);
		if (impactJson.size() > 0) {
			obj.add("impacts", impactJson);
		}

		return obj;
	}
}
