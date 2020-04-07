package org.openlca.app.tools.mapping.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.core.model.descriptors.UnitDescriptor;
import org.openlca.io.maps.FlowRef;
import org.openlca.jsonld.Json;
import org.python.jline.internal.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Collects flow descriptors from a JSON-LD package.
 */
class JsonRefCollector {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final File file;

	private final List<ZipEntry> flowEntries = new ArrayList<>();
	private final List<ZipEntry> categoryEntries = new ArrayList<>();
	private final List<ZipEntry> propEntries = new ArrayList<>();
	private final List<ZipEntry> unitEntries = new ArrayList<>();

	// flow ID -> category path
	private final Map<String, String> categoryPaths = new HashMap<>();

	// property ID -> property name
	private final Map<String, String> propertyNames = new HashMap<>();

	// property ID -> unit ID
	private final Map<String, String> propertyUnits = new HashMap<>();

	// unit ID -> unit name
	private final Map<String, String> unitNames = new HashMap<>();

	JsonRefCollector(File file) {
		this.file = file;
	}

	List<FlowRef> collect() {
		log.trace("collect flow references from {}", file);
		try (ZipFile zip = new ZipFile(file)) {
			scanEntries(zip);
			categories(zip);
			units(zip);
			return buildFlows(zip);
		} catch (Exception e) {
			log.error("failed to collect flows from"
					+ " zip file " + file, e);
			return Collections.emptyList();
		}
	}

	private List<FlowRef> buildFlows(ZipFile zip) {
		List<FlowRef> flowRefs = new ArrayList<>();
		for (ZipEntry e : flowEntries) {
			JsonObject obj = unpack(e, zip);
			if (obj == null)
				continue;
			String id = Json.getString(obj, "@id");
			if (id == null)
				continue;
			FlowRef ref = new FlowRef();
			ref.flow = new FlowDescriptor();
			ref.flow.refId = id;
			ref.flow.name = Json.getString(obj, "name");
			ref.flow.flowType = Json.getEnum(obj, "flowType", FlowType.class);

			// the category
			String catID = Json.getRefId(obj, "category");
			ref.flowCategory = categoryPaths.get(catID);

			// find the reference flow property
			String propID = null;
			JsonArray props = Json.getArray(obj, "flowProperties");
			if (props != null) {
				for (JsonElement elem : props) {
					if (!elem.isJsonObject())
						continue;
					JsonObject prop = elem.getAsJsonObject();
					boolean isRef = Json.getBool(prop,
							"referenceFlowProperty", false);
					if (!isRef)
						continue;
					propID = Json.getRefId(prop, "flowProperty");
					break;
				}
			}
			if (propID != null) {
				ref.property = new FlowPropertyDescriptor();
				ref.property.refId = propID;
				ref.property.name = propertyNames.get(propID);

				String unitID = propertyUnits.get(propID);
				if (unitID != null) {
					ref.unit = new UnitDescriptor();
					ref.unit.refId = unitID;
					ref.unit.name = unitNames.get(unitID);
				}
			}
			flowRefs.add(ref);
		}
		return flowRefs;
	}

	private void scanEntries(ZipFile zip) {
		log.trace("scan zip entries");
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry e = entries.nextElement();
			String name = e.getName();
			if (name.startsWith("flows/")) {
				flowEntries.add(e);
			} else if (name.startsWith("categories/")) {
				categoryEntries.add(e);
			} else if (name.startsWith("flow_properties/")) {
				propEntries.add(e);
			} else if (name.startsWith("unit_groups/")) {
				unitEntries.add(e);
			}
		}
	}

	private void categories(ZipFile zip) {
		HashMap<String, String> names = new HashMap<>();
		HashMap<String, String> parents = new HashMap<>();
		for (ZipEntry e : categoryEntries) {
			JsonObject obj = unpack(e, zip);
			if (obj == null)
				continue;
			String type = Json.getString(obj, "modelType");
			if (!"FLOW".equals(type))
				continue;
			String id = Json.getString(obj, "@id");
			String name = Json.getString(obj, "name");
			if (id == null || name == null)
				continue;
			names.put(id, name);

			JsonObject parent = Json.getObject(obj, "category");
			if (parent == null)
				continue;
			String parentID = Json.getString(parent, "@id");
			if (parentID != null) {
				parents.put(id, parentID);
			}

		}

		for (Map.Entry<String, String> e : names.entrySet()) {
			String id = e.getKey();
			StringBuilder path = new StringBuilder(e.getValue());
			String parent = parents.get(id);
			while (parent != null) {
				String pname = names.get(parent);
				if (pname == null)
					break;
				path.insert(0, pname + "/");
				parent = parents.get(parent);
			}
			categoryPaths.put(id, path.toString());
		}
	}

	private void units(ZipFile zip) {

		// propUnitGroups: property -> unit group
		HashMap<String, String> propUnitGroup = new HashMap<>();
		for (ZipEntry e : propEntries) {
			JsonObject obj = unpack(e, zip);
			if (obj == null)
				continue;
			String id = Json.getString(obj, "@id");
			if (id == null)
				continue;
			propertyNames.put(id, Json.getString(obj, "name"));
			propUnitGroup.put(id, Json.getRefId(obj, "unitGroup"));
		}

		// groupUnit unit group -> reference unit
		HashMap<String, String> groupUnit = new HashMap<>();
		for (ZipEntry e : unitEntries) {
			JsonObject obj = unpack(e, zip);
			if (obj == null)
				continue;
			String groupID = Json.getString(obj, "@id");
			if (groupID == null)
				continue;

			JsonArray units = Json.getArray(obj, "units");
			if (units == null)
				continue;
			for (JsonElement elem : units) {
				if (!elem.isJsonObject())
					continue;
				JsonObject unit = elem.getAsJsonObject();
				boolean isRef = Json.getBool(unit, "referenceUnit", false);
				if (!isRef)
					continue;
				String id = Json.getString(unit, "@id");
				if (id == null)
					continue;
				groupUnit.put(groupID, id);
				unitNames.put(id, Json.getString(unit, "name"));
				break;
			}
		}

		for (Map.Entry<String, String> e : propUnitGroup.entrySet()) {
			String propID = e.getKey();
			String groupID = e.getValue();
			String unitID = groupUnit.get(groupID);
			propertyUnits.put(propID, unitID);
		}
	}

	private JsonObject unpack(ZipEntry e, ZipFile zip) {
		try (InputStream in = zip.getInputStream(e);
				BufferedInputStream buffer = new BufferedInputStream(in);
				Reader r = new InputStreamReader(buffer, "utf-8")) {
			return new Gson().fromJson(r, JsonObject.class);
		} catch (Exception ex) {
			log.warn("failed to read JSON object from {}", e);
			return null;
		}
	}
}
