package org.openlca.app.editors.results.openepd;

import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

class EpdDescriptor {

	private String id;
	private String name;
	private String publicationDate;

	static EpdDescriptor from(JsonObject obj) {
		var d = new EpdDescriptor();
		if (obj == null)
			return d;
		d.id = Json.getString(obj, "id");
		d.name = Json.getString(obj, "name");
		d.publicationDate = Json.getString(obj, "date_of_issue");
		return d;
	}

	String id() {
		return id;
	}

	String name() {
		return name;
	}

	String publicationDate() {
		return publicationDate;
	}
}
