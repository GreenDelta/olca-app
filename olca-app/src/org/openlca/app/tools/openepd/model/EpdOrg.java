package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

/**
 * An organisation description in the openEPD format.
 */
public class EpdOrg implements Jsonable {

	/**
	 * A web domain name owned by organization. Typically, is the org's home
	 * website address without www and http. Domains are case-insensitive.
	 */
	public String webDomain;

	/**
	 * The name of the organization.
	 */
	public String name;

	/**
	 * Reference to this Org's JSON object
	 */
	public String ref;

	/**
	 * Organization that controls this organization
	 */
	public EpdOrg owner;

	/**
	 * List of other names for the organization.
	 */
	public final List<String> altNames = new ArrayList<>(0);

	public static Optional<EpdOrg> fromJson(JsonElement elem) {
		if (elem == null || !elem.isJsonObject())
			return Optional.empty();
		var obj = elem.getAsJsonObject();
		var org = new EpdOrg();
		org.webDomain = Json.getString(obj, "web_domain");
		org.name = Json.getString(obj, "name");
		org.ref = Json.getString(obj, "ref");
		var ownerObj = Json.getObject(obj, "owner");
		if (ownerObj != null) {
			org.owner = EpdOrg.fromJson(ownerObj).orElse(null);
		}
		var names = Json.getArray(obj, "alt_names");
		if (names != null) {
			Json.stream(names)
				.filter(JsonElement::isJsonPrimitive)
				.map(JsonElement::getAsString)
				.forEach(org.altNames::add);
		}
		return Optional.of(org);
	}

	@Override
	public JsonObject toJson() {
		var obj = new JsonObject();
		Json.put(obj, "web_domain", webDomain);
		Json.put(obj, "name", name);
		Json.put(obj, "ref", ref);
		Util.put(obj, "owner", owner);
		if (!altNames.isEmpty()) {
			var array = new JsonArray();
			altNames.forEach(array::add);
			Json.put(obj, "alt_names", array);
		}
		return obj;
	}
}
