package org.openlca.app.editors.graphical.layout;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.db.Cache;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;
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

	private static void writeAsJson(List<NodeLayoutInfo> layoutInfo, File toFile) throws IOException {
		JsonWriter writer = new JsonWriter(new FileWriter(toFile));
		writer.beginObject();
		writer.name("nodes");
		writer.beginArray();
		for (NodeLayoutInfo layout : layoutInfo)
			writeAsJson(layout, writer);
		writer.endArray();
		writer.endObject();
		writer.flush();
		writer.close();
	}

	private static void writeAsJson(NodeLayoutInfo layout, JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name("id");
		writer.value(layout.getId());
		writer.name("x");
		writer.value(layout.getLocation().x);
		writer.name("y");
		writer.value(layout.getLocation().y);
		writer.name("minimized");
		writer.value(layout.isMinimized());
		writer.name("expandedLeft");
		writer.value(layout.isExpandedLeft());
		writer.name("expandedRight");
		writer.value(layout.isExpandedRight());
		writer.name("marked");
		writer.value(layout.isMarked());
		writer.endObject();
	}

	public static boolean loadLayout(ProductSystemNode node) throws NodeLayoutException {
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

	private static void apply(NodeLayoutInfo layout, ProductSystemNode model) throws NodeLayoutException {
		ProcessNode node = model.getProcessNode(layout.getId());
		if (node != null) {
			node.apply(layout);
			return;
		}
		ProcessDescriptor descriptor = Cache.getEntityCache().get(ProcessDescriptor.class, layout.getId());
		if (descriptor == null)
			return;
		node = new ProcessNode(descriptor);
		model.add(node);
		node.apply(layout);
		model.editor.createNecessaryLinks(node);
	}

	private static List<NodeLayoutInfo> parseJson(File fromFile) throws IOException {
		List<NodeLayoutInfo> layoutInfo = new ArrayList<>();
		JsonReader reader = new JsonReader(new FileReader(fromFile));
		reader.beginObject();
		reader.nextName();
		reader.beginArray();
		while (reader.hasNext())
			layoutInfo.add(parseLayoutInfo(reader));
		reader.endArray();
		reader.endObject();
		reader.close();
		return layoutInfo;
	}

	private static NodeLayoutInfo parseLayoutInfo(JsonReader reader) throws IOException {
		reader.beginObject();
		reader.nextName();
		long id = reader.nextLong();
		reader.nextName();
		int x = reader.nextInt();
		reader.nextName();
		int y = reader.nextInt();
		reader.nextName();
		boolean minimized = reader.nextBoolean();
		reader.nextName();
		boolean expandedLeft = reader.nextBoolean();
		reader.nextName();
		boolean expandedRight = reader.nextBoolean();
		reader.nextName();
		boolean marked = reader.nextBoolean();
		reader.endObject();
		return new NodeLayoutInfo(id, x, y, minimized, expandedLeft, expandedRight, marked);
	}

	private static File createLayoutFile(ProductSystemNode node) throws IOException {
		File file = getLayoutFile(node.getProductSystem());
		if (file.exists())
			file.delete();
		file.createNewFile();
		return file;
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
