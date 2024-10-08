package org.openlca.app.editors.graphical;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.graphical.layouts.NodeLayoutInfo;
import org.openlca.app.editors.graphical.layouts.StickyNoteLayoutInfo;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.MinMaxComponent;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.core.model.ProductSystem;
import org.openlca.jsonld.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openlca.app.editors.graphical.GraphConfig.CONFIG_PROP;


/**
 * We save the current layout and some settings in an external file of the
 * database folder.
 */
public final class GraphFile {

	static final String KEY_NODES = "nodes";
	static final String KEY_STICKY_NOTES = "sticky-notes";

	private GraphFile() {
	}

	public static void save(GraphEditor editor) {
		var graph = editor != null
				? editor.getModel()
				: null;
		if (graph == null)
			return;

		editor.config.setZoom(editor.getZoomManager().getZoom());
		var viewLocation = editor.getZoomManager().getViewport().getViewLocation();
		editor.config.setViewLocation(viewLocation);
		var rootObj = createJsonArray(editor, graph);
		try {
			Json.write(rootObj, file(editor.getProductSystem()));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(GraphFile.class);
			log.error("Failed to save layout", e);
		}
	}

	public static void clear(GraphEditor editor) {
		try {
			FileUtils.deleteDirectory(DatabaseDir.getDir(editor.getProductSystem()));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(GraphFile.class);
			log.error("Failed to clear saved layout", e);
		}
	}

	public static JsonObject createJsonArray(GraphEditor editor, Graph graph) {
		// add config
		var rootObj = new JsonObject();
		rootObj.add(CONFIG_PROP, editor.config.toJson());

		// add component info's
		var nodeArray = new JsonArray();
		for (var node : graph.getNodes()) {
			var nodeObj = toJson(node);
			if (nodeObj != null)
				nodeArray.add(nodeObj);
		}
		var stickyNoteArray = new JsonArray();
		for (var note : graph.getStickyNotes()) {
			var noteObj = toJson(note);
			if (noteObj != null)
				stickyNoteArray.add(noteObj);
		}
		rootObj.add(KEY_NODES, nodeArray);
		rootObj.add(KEY_STICKY_NOTES, stickyNoteArray);
		return rootObj;
	}

	private static JsonObject toJson(Node node) {
		if (node == null || node.descriptor == null)
			return null;
		var json = new JsonObject();
		json.addProperty("id", node.descriptor.refId);

		setLayoutProperties(json, node);
		json.addProperty("expandedLeft", node.isExpanded(Side.INPUT));
		json.addProperty("expandedRight", node.isExpanded(Side.OUTPUT));

		return json;
	}

	private static void setLayoutProperties(JsonObject json,
		MinMaxComponent component) {
		var size = component.getSize();
		if (size != null) {
			json.addProperty("width",  size.width);
			json.addProperty("height",  size.height);
		}
		var location = component.getLocation();
		if (location != null) {
			json.addProperty("x", location.x);
			json.addProperty("y",  location.y);
		}

		json.addProperty("minimized", component.isMinimized());
	}

	private static JsonObject toJson(StickyNote note) {
		if (note == null)
			return null;
		var json = new JsonObject();
		json.addProperty("title", note.title);
		json.addProperty("content", note.content);

		setLayoutProperties(json, note);

		return json;
	}

	public static GraphConfig getGraphConfig(GraphEditor editor) {
		try {
			var file = GraphFile.file(editor.getProductSystem());
			if (!file.exists())
				return null;
			var rootObj = Json.readObject(file)
				.orElse(null);
			if (rootObj == null)
				return null;

			// apply graph config
			return GraphConfig.fromJson(
				Json.getObject(rootObj, CONFIG_PROP));
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(GraphFile.class);
			log.error("Failed to load config", e);
			return null;
		}
	}

	public static JsonArray getLayout(GraphEditor editor, String element) {
		try {
			// read JSON object from file
			var file = file(editor.getProductSystem());
			if (!file.exists())
				return null;
			var rootObj = Json.readObject(file).orElse(null);
			if (rootObj == null)
				return null;

			// apply node info's
			return Json.getArray(rootObj, element);
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(GraphFile.class);
			log.error("Failed to load layout", e);
			return null;
		}
	}

	public static NodeLayoutInfo toNodeLayoutInfo(JsonObject obj) {
		if (obj == null)
			return null;
		var info = new NodeLayoutInfo();
		info.id = Json.getString(obj, "id");
		info.minimized = Json.getBool(obj, "minimized", false);
		info.location = new Point(
			Json.getInt(obj, "x", Integer.MAX_VALUE),
			Json.getInt(obj, "y", Integer.MAX_VALUE));
		info.size = new Dimension(
			Json.getInt(obj, "width", Node.DEFAULT_SIZE.width),
			Json.getInt(obj, "height", Node.DEFAULT_SIZE.height));
		info.expandedLeft = Json.getBool(obj, "expandedLeft", false);
		info.expandedRight = Json.getBool(obj, "expandedRight", false);
		return info;
	}

	public static StickyNoteLayoutInfo toStickyNoteLayoutInfo(JsonObject obj) {
		if (obj == null)
			return null;
		var info = new StickyNoteLayoutInfo();
		info.minimized = Json.getBool(obj, "minimized", false);
		info.location = new Point(
				Json.getInt(obj, "x", Integer.MAX_VALUE),
				Json.getInt(obj, "y", Integer.MAX_VALUE));
		info.size = new Dimension(
				Json.getInt(obj, "width", Node.DEFAULT_SIZE.width),
				Json.getInt(obj, "height", Node.DEFAULT_SIZE.height));
		info.title = Json.getString(obj, "title");
		info.content = Json.getString(obj, "content");

		return info;
	}

	public static File file(ProductSystem system) {
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
