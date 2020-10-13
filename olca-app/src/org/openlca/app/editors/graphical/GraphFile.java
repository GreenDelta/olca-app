package org.openlca.app.editors.graphical;

import java.io.File;

import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.graphical.layout.NodeLayoutInfo;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProductSystem;
import org.openlca.jsonld.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * We save the current layout and some settings in an external file of the
 * database folder.
 */
public final class GraphFile {

	private GraphFile() {
	}

	public static void save(ProductSystemNode root) {
		if (root == null)
			return;
		try {
			var file = file(root.getProductSystem());
			var nodeArray = new JsonArray();
			for (var node : root.getChildren()) {
				var nodeObj = toJson(node);
				if (nodeObj != null) {
					nodeArray.add(nodeObj);
				}
			}
			var rootObj = new JsonObject();
			rootObj.add("nodes", nodeArray);
			Json.write(rootObj, file);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(GraphFile.class);
			log.error("Failed to save layout", e);
		}
	}

	private static JsonObject toJson(ProcessNode node) {
		if (node == null || node.process == null)
			return null;
		var xy = node.getXyLayoutConstraints();
		var json = new JsonObject();
		json.addProperty("id", node.process.id);
		json.addProperty("x", xy != null ? xy.x : 0);
		json.addProperty("y", xy != null ? xy.y : 0);
		json.addProperty("minimized", node.isMinimized());
		json.addProperty("expandedLeft", node.isExpandedLeft());
		json.addProperty("expandedRight", node.isExpandedLeft());
		json.addProperty("marked", node.isMarked());
		return json;
	}

	public static boolean apply(GraphEditor editor) {
		if (editor == null)
			return false;
		var root = editor.getModel();
		if (root == null || root.getProductSystem() == null)
			return false;
		var file = file(root.getProductSystem());
		if (!file.exists())
			return false;
		try {
			var rootObj = Json.readObject(file)
					.orElse(null);
			if (rootObj == null)
				return false;

			var nodeArray = Json.getArray(rootObj, "nodes");
			applyNodes(nodeArray, root);

			return true;
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(GraphFile.class);
			log.error("Failed to load layout", e);
			return false;
		}
	}

	private static void applyNodes(JsonArray array, ProductSystemNode root) {
		if (array == null || root == null)
			return;
		for (var elem : array) {
			if (!elem.isJsonObject())
				continue;
			var obj = elem.getAsJsonObject();
			var info = toInfo(obj);
			if (info == null)
				continue;
			var node = root.getProcessNode(info.id);
			if (node != null) {
				node.apply(info);
				continue;
			}
			node = ProcessNode.create(root.editor, info.id);
			if (node == null)
				continue;
			root.add(node);
			node.apply(info);
			root.editor.createNecessaryLinks(node);
		}
	}

	private static NodeLayoutInfo toInfo(JsonObject obj) {
		if (obj == null)
			return null;
		var info = new NodeLayoutInfo();
		info.id = Json.getLong(obj, "id", 0);
		info.x = Json.getInt(obj, "x", 0);
		info.y = Json.getInt(obj, "y", 0);
		info.minimized = Json.getBool(obj, "minimized", false);
		info.expandedLeft = Json.getBool(obj, "expandedLeft", false);
		info.expandedRight = Json.getBool(obj, "expandedRight", false);
		info.marked = Json.getBool(obj, "marked", false);
		return info;
	}

	private static File file(ProductSystem system) {
		File dir = DatabaseDir.getDir(system);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new RuntimeException(
						"failed to create folder " + dir);
			}
		}
		return new File(dir, "layout.json");
	}

}
