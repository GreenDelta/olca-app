package org.openlca.app.editors.lcia.geo;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.jsonld.Json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Describes the binding of regionalized characterization factors of a flow via
 * a formula with parameters of geographic features.
 */
class GeoFlowBinding {
	final Flow flow;
	String formula;

	GeoFlowBinding(Flow flow) {
		this.flow = flow;
		this.formula = "1";
	}

	static GeoFlowBinding fromJson(JsonObject obj, IDatabase db) {
		if (obj == null || db == null)
			return null;
		JsonElement flowObj = obj.get("flow");
		if (flowObj == null || !flowObj.isJsonObject())
			return null;
		String flowID = Json.getString(flowObj.getAsJsonObject(), "@id");
		if (flowID == null)
			return null;
		FlowDao dao = new FlowDao(db);
		Flow flow = dao.getForRefId(flowID);
		if (flow == null)
			return null;
		GeoFlowBinding b = new GeoFlowBinding(flow);
		b.formula = Json.getString(obj, "formula");
		return b;
	}

	JsonObject toJson() {
		JsonObject obj = new JsonObject();
		if (flow != null) {
			JsonObject flowObj = new JsonObject();
			flowObj.addProperty("@type", "Flow");
			flowObj.addProperty("@id", flow.refId);
			flowObj.addProperty("name", flow.name);
			obj.add("flow", flowObj);
		}
		obj.addProperty("formula", formula);
		return obj;
	}

}
