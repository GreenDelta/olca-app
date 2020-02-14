package org.openlca.app.tools.mapping.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
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

	private List<ZipEntry> flowEntries = new ArrayList<>();
	private List<ZipEntry> categoryEntries = new ArrayList<>();
	private List<ZipEntry> propEntries = new ArrayList<>();
	private List<ZipEntry> unitEntries = new ArrayList<>();

	private Map<String, String> categoryPaths;

	JsonRefCollector(File file) {
		this.file = file;
	}

	List<FlowRef> collect() {
		log.trace("collect flow references from {}", file);

		List<FlowRef> refs = new ArrayList<>();
		try (ZipFile zip = new ZipFile(file)) {
			scanEntries(zip);
			categoryPaths = getCategoriePaths(zip);
		} catch (Exception e) {
			log.error("failed to collect flows from"
					+ " zip file " + file, e);
		}

		return refs;
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

			// the category ID
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
				}
			}
			if (propID != null) {
				ref.property = new FlowPropertyDescriptor();
				ref.property.refId = propID;
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

	private Map<String, String> getCategoriePaths(ZipFile zip) {
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

		HashMap<String, String> paths = new HashMap<>();
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
			paths.put(id, path.toString());
		}
		return paths;
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

	public static void main(String[] args) {
		SimpleDateFormat time = new SimpleDateFormat("hh:mm:ss:SSS");
		System.out.println("Start @ " + time.format(new Date()));
		File f = new File("C:\\Users\\ms\\Desktop\\rems\\uslci_map.zip");
		new JsonRefCollector(f).collect();
		System.out.println("Finished @ " + time.format(new Date()));
	}

}
