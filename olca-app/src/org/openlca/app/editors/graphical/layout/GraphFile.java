package org.openlca.app.editors.graphical.layout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

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
		List<NodeLayoutInfo> layoutInfo = new ArrayList<>();
		for (ProcessNode node : root.getChildren()) {
			if (!node.isVisible())
				continue;
			layoutInfo.add(new NodeLayoutInfo(node));
		}
		try {
			var file = file(root.getProductSystem());
			try (var s = new FileOutputStream(file);
				 var w = new OutputStreamWriter(s, StandardCharsets.UTF_8);
				 var buffer = new BufferedWriter(w);
				 var json = new JsonWriter(buffer)) {
				json.beginObject();
				json.name("nodes");
				json.beginArray();
				for (var node : root.getChildren()) {
					writeNode(json, node);
				}
				json.endArray();
				json.endObject();
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(GraphFile.class);
			log.error("Failed to save layout", e);
		}
	}

	private static void writeNode(JsonWriter w, ProcessNode node)
		throws IOException{
		if (node == null || node.process == null)
			return;
		var xy = node.getXyLayoutConstraints();
		w.beginObject();
		w.name("id");
		w.value(node.process.id);
		w.name("x");
		w.value(xy != null ? xy.x : 0);
		w.name("y");
		w.value(xy != null ? xy.y : 0);
		w.name("minimized");
		w.value(node.isMinimized());
		w.name("expandedLeft");
		w.value(node.isExpandedLeft());
		w.name("expandedRight");
		w.value(node.isExpandedRight());
		w.name("marked");
		w.value(node.isMarked());
		w.endObject();
	}

	public static boolean apply(ProductSystemNode node) {
		if (node == null || node.getProductSystem() == null)
			return false;
		File file = file(node.getProductSystem());
		if (!file.exists())
			return false;
		try {
			List<NodeLayoutInfo> layoutInfo = parseJson(file);
			for (NodeLayoutInfo layout : layoutInfo)
				apply(layout, node);
			return true;
		} catch (IOException e) {
			Logger log = LoggerFactory.getLogger(GraphFile.class);
			log.error("Failed to load layout", e);
			return false;
		}
	}

	private static void apply(NodeLayoutInfo info, ProductSystemNode model) {
		ProcessNode node = model.getProcessNode(info.id);
		if (node != null) {
			node.apply(info);
			return;
		}
		node = ProcessNode.create(model.editor, info.id);
		if (node == null)
			return;
		model.add(node);
		node.apply(info);
		model.editor.createNecessaryLinks(node);
	}

	private static List<NodeLayoutInfo> parseJson(
			File fromFile) throws IOException {
		List<NodeLayoutInfo> layoutInfo = new ArrayList<>();
		JsonReader r = new JsonReader(new FileReader(fromFile));
		r.beginObject();
		r.nextName();
		r.beginArray();
		while (r.hasNext()) {
			layoutInfo.add(parseLayoutInfo(r));
		}
		r.endArray();
		r.endObject();
		r.close();
		return layoutInfo;
	}

	private static NodeLayoutInfo parseLayoutInfo(
			JsonReader r) throws IOException {
		NodeLayoutInfo info = new NodeLayoutInfo();
		r.beginObject();
		r.nextName();
		info.id = r.nextLong();
		r.nextName();
		info.x = r.nextInt();
		r.nextName();
		info.y = r.nextInt();
		r.nextName();
		info.minimized = r.nextBoolean();
		r.nextName();
		info.expandedLeft = r.nextBoolean();
		r.nextName();
		info.expandedRight = r.nextBoolean();
		r.nextName();
		info.marked = r.nextBoolean();
		r.endObject();
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
