package org.openlca.app.tools.openepd.model;

import java.time.LocalDate;
import java.util.Optional;

import com.google.gson.JsonElement;
import org.openlca.jsonld.Json;

public class Ec3Pcr {

	public String id;
	public String ref;
	public String name;
	public String shortName;
	public String version;
	public LocalDate dateOfIssue;
	public LocalDate dateValidityEnds;

	public static Optional<Ec3Pcr> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var pcr = new Ec3Pcr();
		pcr.id = Json.getString(obj, "id");
		pcr.ref = Json.getString(obj, "ref");
		pcr.name = Json.getString(obj, "name");
		pcr.shortName = Json.getString(obj, "short_name");
		pcr.version = Json.getString(obj, "version");
		pcr.dateOfIssue = Util.getDate(obj, "date_of_issue");
		pcr.dateValidityEnds = Util.getDate(obj, "valid_until");
		return Optional.of(pcr);
	}

}
