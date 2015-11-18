package org.openlca.app.cloud.index;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.core.model.ModelType;

// NOT SYNCHRONIZED //
public class DiffIndex {

	private File file;
	private DB db;
	private Map<String, Diff> index;
	private Map<String, Set<String>> changedTopLevelElements;

	public static DiffIndex getFor(RepositoryClient client) {
		RepositoryConfig config = client.getConfig();
		return new DiffIndex(new File(config.getDatabase()
				.getFileStorageLocation(), "cloud/" + config.getRepositoryId()));
	}

	private DiffIndex(File indexDirectory) {
		if (!indexDirectory.exists())
			indexDirectory.mkdirs();
		file = new File(indexDirectory, "indexfile");
		createDb(file);
	}

	private void createDb(File file) {
		db = DBMaker.fileDB(file).lockDisable().closeOnJvmShutdown().make();
		index = db.hashMap("diffIndex");
		changedTopLevelElements = db.hashMap("changedTopLevelElements");
	}

	public void close() {
		if (!db.isClosed())
			db.close();
	}

	void add(DatasetDescriptor descriptor) {
		Diff diff = index.get(descriptor.getRefId());
		if (diff != null)
			return;
		diff = new Diff(descriptor, DiffType.NO_DIFF);
		index.put(descriptor.getRefId(), diff);
	}

	void update(DatasetDescriptor descriptor, DiffType newType) {
		Diff diff = index.get(descriptor.getRefId());
		if (diff.type == DiffType.NEW && newType == DiffType.DELETED) {
			// user added something and then deleted it again
			remove(descriptor.getRefId());
			return;
		}
		updateDiff(diff, descriptor, newType);
	}

	private void updateDiff(Diff diff, DatasetDescriptor descriptor,
			DiffType newType) {
		diff.type = newType;
		if (newType == DiffType.NO_DIFF) {
			updateParents(diff, false);
			diff.descriptor = descriptor;
			diff.changed = null;
		} else {
			diff.changed = descriptor;
			updateParents(diff, true);
		}
		if (descriptor.getCategoryRefId() == null)
			updateChangedTopLevelElements(descriptor, newType);
		index.put(descriptor.getRefId(), diff);
	}

	private void updateChangedTopLevelElements(DatasetDescriptor descriptor,
			DiffType newType) {
		String type = descriptor.getCategoryType().name();
		Set<String> elements = changedTopLevelElements.get(type);
		if (elements == null && newType != DiffType.NO_DIFF) 
			elements = new HashSet<>();
		if (newType == DiffType.NO_DIFF)
			elements.remove(descriptor.getRefId());
		else
			elements.add(descriptor.getRefId());
		if (elements.isEmpty())
			changedTopLevelElements.remove(type);
		else
			changedTopLevelElements.put(type, elements);
	}

	public Diff get(String key) {
		return index.get(key);
	}

	public List<Diff> getChanged() {
		List<Diff> changed = new ArrayList<>();
		for (Diff diff : index.values())
			if (diff.hasChanged())
				changed.add(diff);
		return changed;
	}

	public boolean hasChanged(ModelType type) {
		Set<String> elements = changedTopLevelElements.get(type.name());
		return elements != null && !elements.isEmpty();
	}

	void remove(String key) {
		Diff diff = index.remove(key);
		updateChangedTopLevelElements(diff.getDescriptor(), DiffType.NO_DIFF);
		updateParents(diff, false);
	}

	private void updateParents(Diff diff, boolean add) {
		if (diff.changed != null) // case 1)
			updateParents(diff.changed, add);
		if (diff.descriptor != null) // case 2)
			updateParents(diff.descriptor, add);
	}

	private void updateParents(DatasetDescriptor descriptor, boolean add) {
		String parentId = descriptor.getCategoryRefId();
		while (parentId != null) {
			Diff parent = index.get(parentId);
			if (add)
				parent.changedChildren.add(descriptor.getRefId());
			else
				parent.changedChildren.remove(descriptor.getRefId());
			index.put(parentId, parent);
			parentId = parent.descriptor.getCategoryRefId();
		}
	}

	void commit() {
		db.commit();
	}

}
