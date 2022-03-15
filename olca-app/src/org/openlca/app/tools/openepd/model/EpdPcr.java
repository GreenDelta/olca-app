package org.openlca.app.tools.openepd.model;

import java.time.LocalDate;
import java.util.Optional;

import com.google.gson.JsonElement;
import org.openlca.jsonld.Json;

/**
 * A description of a PCR (product category rule) in the openEPD format.
 */
public class EpdPcr {

	/**
	 * The unique ID for this PCR. To ensure global uniqueness, should be
	 * registered at open-xpd-uuid.cqd.io/register or a coordinating registry.
	 */
	public String id;

	/**
	 * Document ID or code created by issuer.
	 */
	public String issuerDocId;

	/**
	 * Full document name as listed in source document
	 */
	public String name;

	/**
	 * A shortened name without boilerplate text
	 */
	public String shortName;

	/**
	 * Document version, as expressed in document.
	 */
	public String version;

	/**
	 * First day on which the document is valid.
	 */
	public LocalDate dateOfIssue;

	/**
	 * Last day on which the document is valid.
	 */
	public LocalDate dateValidityEnds;

	/**
	 * Reference to this PCR's JSON object
	 */
	public String ref;


	public static Optional<EpdPcr> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var pcr = new EpdPcr();
		pcr.id = Json.getString(obj, "id");
		pcr.issuerDocId = Json.getString(obj, "issuer_doc_id");
		pcr.ref = Json.getString(obj, "ref");
		pcr.name = Json.getString(obj, "name");
		pcr.shortName = Json.getString(obj, "short_name");
		pcr.version = Json.getString(obj, "version");
		pcr.dateOfIssue = Util.getDate(obj, "date_of_issue");
		pcr.dateValidityEnds = Util.getDate(obj, "valid_until");
		return Optional.of(pcr);
	}

}
