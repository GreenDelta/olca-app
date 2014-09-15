package org.openlca.app.editors.graphical.layout;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseFolder;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.descriptors.ProcessDescriptor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class NodeLayoutStore {

	private NodeLayoutStore() {
	}

	public static void saveLayout(ProductSystemNode model) {
		if (model == null)
			return;
		List<NodeLayoutInfo> layoutInfo = new ArrayList<>();
		for (ProcessNode node : model.getChildren())
			if (node.isVisible())
				layoutInfo.add(new NodeLayoutInfo(node));
		try {
			File layoutFile = createLayoutFile(model);
			writeAsJson(layoutInfo, layoutFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeAsJson(List<NodeLayoutInfo> layoutInfo, File toFile)
			throws IOException {
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

	private static void writeAsJson(NodeLayoutInfo layout, JsonWriter writer)
			throws IOException {
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

	public static boolean loadLayout(ProductSystemNode model) {
		if (model == null)
			return false;
		File layoutFile = getLayoutFile(model.getProductSystem().getRefId());
		if (!layoutFile.exists())
			return false;
		try {
			List<NodeLayoutInfo> layoutInfo = parseJson(layoutFile);
			for (NodeLayoutInfo layout : layoutInfo)
				apply(layout, model);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static void apply(NodeLayoutInfo layout, ProductSystemNode model) {
		ProcessNode node = model.getProcessNode(layout.getId());
		if (node == null) {
			ProcessDescriptor descriptor = Cache.getEntityCache().get(
					ProcessDescriptor.class, layout.getId());
			node = new ProcessNode(descriptor);
			model.add(node);
			node.apply(layout);
			model.getEditor().createNecessaryLinks(node);
		} else {
			node.apply(layout);
		}
	}

	private static List<NodeLayoutInfo> parseJson(File fromFile)
			throws IOException {
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

	private static NodeLayoutInfo parseLayoutInfo(JsonReader reader)
			throws IOException {
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
		return new NodeLayoutInfo(id, x, y, minimized, expandedLeft,
				expandedRight, marked);
	}

	private static File createLayoutFile(ProductSystemNode model)
			throws IOException {
		File file = getLayoutFile(model.getProductSystem().getRefId());
		if (file.exists())
			file.delete();
		file.createNewFile();
		return file;
	}

	public static File getLayoutFile(String refId) {
		File layoutStore = getLayoutStore();
		File layoutFile = new File(layoutStore, refId + ".json");
		return layoutFile;
	}

	private static File getLayoutStore() {
		File storage = DatabaseFolder.getFileStorageLocation(Database.get());
		File layoutStore = new File(storage, "layouts");
		if (!layoutStore.exists())
			layoutStore.mkdirs();
		return layoutStore;
	}

	public static void deleteLayout(String refId) {
		File layoutFile = getLayoutFile(refId);
		if (!layoutFile.exists())
			return;
		if (!layoutFile.delete())
			layoutFile.deleteOnExit();
	}
}
