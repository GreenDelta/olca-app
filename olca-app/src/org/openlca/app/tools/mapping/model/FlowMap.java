package org.openlca.app.tools.mapping.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.jsonld.Json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class FlowMap extends BaseDescriptor {

	/** Description of the source system. */
	public BaseDescriptor source;

	/** Description of the target system. */
	public BaseDescriptor target;

	public final List<FlowMapEntry> entries = new ArrayList<>();

	public static FlowMap from(JsonObject obj) {
		FlowMap map = new FlowMap();
		map.name = Json.getString(obj, "name");
		map.description = Json.getString(obj, "name");

		map.source = new BaseDescriptor();
		fillRef(Json.getObject(obj, "source"), map.source);
		map.target = new BaseDescriptor();
		fillRef(Json.getObject(obj, "target"), map.target);

		JsonArray array = Json.getArray(obj, "mappings");
		if (array != null) {
			for (JsonElement e : array) {
				if (!e.isJsonObject())
					continue;
				JsonObject eObj = e.getAsJsonObject();
				FlowMapEntry entry = new FlowMapEntry();
				entry.sourceFlow = readFlowRef(
						Json.getObject(eObj, "from"));
				entry.targetFlow = readFlowRef(
						Json.getObject(eObj, "to"));
				entry.factor = Json.getDouble(
						eObj, "conversionFactor", 1.0);
				map.entries.add(entry);
			}
		}
		return map;
	}

	private static FlowRef readFlowRef(JsonObject obj) {
		if (obj == null)
			return null;
		FlowRef ref = new FlowRef();
		ref.flow = new FlowDescriptor();
		fillRef(Json.getObject(obj, "flow"), ref.flow);

		JsonObject fp = Json.getObject(obj, "flowProperty");
		if (fp != null) {
			ref.flowProperty = new BaseDescriptor();
			fillRef(fp, ref.flowProperty);
		}
		JsonObject u = Json.getObject(obj, "unit");
		if (u != null) {
			ref.unit = new BaseDescriptor();
			fillRef(u, ref.unit);
		}

		return ref;
	}

	private static void fillRef(JsonObject obj, BaseDescriptor d) {
		if (obj == null || d == null)
			return;
		d.name = Json.getString(obj, "name");
		d.description = Json.getString(obj, "description");
		d.refId = Json.getString(obj, "@id");
		// TODO: last change + version
		// TODO: category path
		// flow type
		// location ... in case of flow references
	}

}
