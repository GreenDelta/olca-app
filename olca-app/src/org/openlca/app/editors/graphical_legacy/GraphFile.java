package org.openlca.app.editors.graphical_legacy;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.graphical_legacy.layout.NodeLayoutInfo;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Result;
import org.openlca.core.model.descriptors.RootDescriptor;
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
		json.addProperty("id", node.process.refId);

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
			config.copyTo(editor.config);

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

		Function<String, RootDescriptor> provider = refId -> {
			var db = Database.get();
			if (db == null)
				return null;
			for (var c : List.of(Process.class, ProductSystem.class, Result.class)) {
				if (db.getDescriptor(c, refId) instanceof RootDescriptor d)
					return d;
			}
			return null;
		};

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

			// we changed the ID to a string in openLCA v2; to be a bit
			// backwards compatible we try the long ID too
			try {
				var id = Long.parseLong(info.id);
				node = root.getProcessNode(id);
				if (node != null) {
					node.apply(info);
					continue;
				}
				node = ProcessNode.create(root.editor, id);
			} catch (Exception ignored) {
			}

			if (node == null) {
				var p = provider.apply(info.id);
				if (p != null) {
					node = new ProcessNode(root.editor, p);
				}
			}
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
		info.id = Json.getString(obj, "id");
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
