package org.openlca.app.editors.graphical.layout;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

public final class NodeLayoutStore {

	private NodeLayoutStore() {
	}

	public static void saveLayout(ProductSystemNode model) {
		if (model == null)
			return;
		List<NodeLayoutInfo> layoutInfo = new ArrayList<>();
		for (ProcessNode node : model.getChildren()) {
			if (!node.isVisible())
				continue;
			layoutInfo.add(new NodeLayoutInfo(node));
		}
		try {
			File layoutFile = createLayoutFile(model);
			writeAsJson(layoutInfo, layoutFile);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(NodeLayoutStore.class);
			log.error("Failed to save layout", e);
		}
	}

	private static void writeAsJson(List<NodeLayoutInfo> layoutInfo,
			File file) throws IOException {
		JsonWriter w = new JsonWriter(new FileWriter(file));
		w.beginObject();
		w.name("nodes");
		w.beginArray();
		for (NodeLayoutInfo layout : layoutInfo)
			writeAsJson(layout, w);
		w.endArray();
		w.endObject();
		w.flush();
		w.close();
	}

	private static void writeAsJson(NodeLayoutInfo info,
			JsonWriter w) throws IOException {
		w.beginObject();
		w.name("id");
		w.value(info.id);
		w.name("x");
		w.value(info.getLocation().x);
		w.name("y");
		w.value(info.getLocation().y);
		w.name("minimized");
		w.value(info.minimized);
		w.name("expandedLeft");
		w.value(info.expandedLeft);
		w.name("expandedRight");
		w.value(info.expandedRight);
		w.name("marked");
		w.value(info.marked);
		w.endObject();
	}

	public static boolean loadLayout(ProductSystemNode node) {
		if (node == null || node.getProductSystem() == null)
			return false;
		File file = getLayoutFile(node.getProductSystem());
		if (!file.exists())
			return false;
		try {
			List<NodeLayoutInfo> layoutInfo = parseJson(file);
			for (NodeLayoutInfo layout : layoutInfo)
				apply(layout, node);
			return true;
		} catch (IOException e) {
			Logger log = LoggerFactory.getLogger(NodeLayoutStore.class);
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
		node = ProcessNode.create(info.id);
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

	private static File createLayoutFile(
			ProductSystemNode node) throws IOException {
		File f = getLayoutFile(node.getProductSystem());
		if (f.exists())
			f.delete();
		f.createNewFile();
		return f;
	}

	private static File getLayoutFile(ProductSystem system) {
		File dir = DatabaseDir.getDir(system);
		if (!dir.exists())
			dir.mkdirs();
		File layoutFile = new File(dir, "layout.json");
		return layoutFile;
	}

	public static class NodeLayoutException extends Exception {

		private static final long serialVersionUID = -6387346828566795215L;

	}

}
