package org.openlca.app.editors.lcia.geo;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.jsonld.Json;

import com.google.gson.JsonObject;

/**
 * Describes a binding of numeric properties of geometric features to the
 * regionalized characterization factor of a flow. This binding is basically
 * just a formula with references to numeric properties of geometric features.
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
		var flowObj = obj.get("flow");
		if (flowObj == null || !flowObj.isJsonObject())
			return null;
		var flowID = Json.getString(flowObj.getAsJsonObject(), "@id");
		if (flowID == null)
			return null;
		var dao = new FlowDao(db);
		var flow = dao.getForRefId(flowID);
		if (flow == null)
			return null;
		var b = new GeoFlowBinding(flow);
		b.formula = Json.getString(obj, "formula");
		return b;
	}

	JsonObject toJson() {
		var obj = new JsonObject();
		if (flow != null) {
			var flowObj = new JsonObject();
			flowObj.addProperty("@type", "Flow");
			flowObj.addProperty("@id", flow.refId);
			flowObj.addProperty("name", flow.name);
			obj.add("flow", flowObj);
		}
		obj.addProperty("formula", formula);
		return obj;
	}

	/**
	 * Calculates the default value of this formula using the default values of
	 * the given properties. Returns null if the evaluation of the formula failed.
	 */
	Double defaultValueOf(Iterable<GeoProperty> properties) {
		var interpreter = new FormulaInterpreter();
		for (var p : properties) {
			interpreter.bind(p.identifier, p.defaultValue);
		}
		try {
			return interpreter.eval(formula);
		} catch (Exception e) {
			return null;
		}
	}
}
