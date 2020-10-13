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

	public static void save(GraphEditor editor) {
		var root = editor != null
				? editor.getModel()
				: null;
		if (root == null)
			return;
		try {

			// add config
			var rootObj = new JsonObject();
			rootObj.add("config", editor.config.toJson());

			// add node infos
			var file = file(root.getProductSystem());
			var nodeArray = new JsonArray();
			for (var node : root.getChildren()) {
				var nodeObj = toJson(node);
				if (nodeObj != null) {
					nodeArray.add(nodeObj);
				}
			}
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
		var json = new JsonObject();
		json.addProperty("id", node.process.id);

		var box = node.getBox();
		if (box != null) {
			json.addProperty("x", box.x);
			json.addProperty("y",  box.y);
			json.addProperty("width",  box.width);
			json.addProperty("height",  box.height);
		}
		
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
		try {

			// read JSON object from file
			var file = file(root.getProductSystem());
			if (!file.exists())
				return false;
			var rootObj = Json.readObject(file)
					.orElse(null);
			if (rootObj == null)
				return false;

			// apply graph config
			var config = GraphConfig.fromJson(
					Json.getObject(rootObj, "config"));
			config.applyOn(editor.config);

			// apply node infos
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
		info.box.x = Json.getInt(obj, "x", 0);
		info.box.y = Json.getInt(obj, "y", 0);
		info.box.width = Json.getInt(obj, "width", 175);
		info.box.height = Json.getInt(obj, "height", 25);
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
