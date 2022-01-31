package org.openlca.app.tools.openepd.model;

import com.google.gson.JsonElement;
import org.openlca.jsonld.Json;
import org.openlca.util.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Ec3Epd {

	public String id;
	public String refUrl;
	public String docType;
	public String language;
	public boolean isPrivate;
	public String declarationUrl;
	public String lcaDiscussion;

	public LocalDate dateOfIssue;
	public LocalDate dateValidityEnds;
	public Ec3Quantity declaredUnit;
	public Ec3Quantity kgPerDeclaredUnit;

	public String productName;
	public String productDescription;
	public final List<Pair<String, String>> productClasses = new ArrayList<>();
	public final List<Ec3ImpactResult> impactResults = new ArrayList<>();

	public static Optional<Ec3Epd> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();

		var obj = elem.getAsJsonObject();
		var epd = new Ec3Epd();

		epd.id = Json.getString(obj, "id");
		epd.refUrl = Json.getString(obj, "ref");
		epd.docType = Json.getString(obj, "doctype");
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

		var classes = Json.getObject(obj, "product_classes");
		if (classes != null) {
			for (var e : classes.entrySet()) {
				var classification = e.getKey();
				var classVal = e.getValue();
				if (!classVal.isJsonPrimitive())
					continue;
				epd.productClasses.add(
					Pair.of(classification, classVal.toString()));
			}
		}

		// impacts
		var impactJson = Json.getObject(obj, "impacts");
		if (impactJson != null) {
			var impactResults = Ec3ImpactResult.fromJson(impactJson);
			epd.impactResults.addAll(impactResults);
		}

		return Optional.of(epd);
	}
}
