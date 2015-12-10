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
import org.openlca.cloud.model.data.Dataset;
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

	public void add(Dataset dataset, long localId) {
		Diff diff = index.get(dataset.getRefId());
		if (diff != null)
			return;
		diff = new Diff(dataset, DiffType.NO_DIFF);
		diff.localId = localId;
		index.put(dataset.getRefId(), diff);
	}

	public void update(Dataset dataset, DiffType newType) {
		Diff diff = index.get(dataset.getRefId());
		if (diff.type == DiffType.NEW && newType == DiffType.DELETED) {
			// user added something and then deleted it again
			remove(dataset.getRefId());
			return;
		}
		updateDiff(diff, dataset, newType);
	}

	private void updateDiff(Diff diff, Dataset dataset, DiffType newType) {
		diff.type = newType;
		if (newType == DiffType.NO_DIFF) {
			updateParents(diff, false);
			diff.dataset = dataset;
			diff.changed = null;
		} else {
			diff.changed = dataset;
			updateParents(diff, true);
		}
		if (dataset.getCategoryRefId() == null)
			updateChangedTopLevelElements(dataset, newType);
		index.put(dataset.getRefId(), diff);
	}

	private void updateChangedTopLevelElements(Dataset dataset, DiffType newType) {
		String type = dataset.getCategoryType().name();
		Set<String> elements = changedTopLevelElements.get(type);
		if (elements == null)
			elements = new HashSet<>();
		if (newType == DiffType.NO_DIFF)
			elements.remove(dataset.getRefId());
		else
			elements.add(dataset.getRefId());
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

	public void remove(String key) {
		Diff diff = index.remove(key);
		updateChangedTopLevelElements(diff.getDataset(), DiffType.NO_DIFF);
		updateParents(diff, false);
	}

	private void updateParents(Diff diff, boolean add) {
		if (diff.changed != null) // case 1)
			updateParents(diff.changed, add);
		if (diff.dataset != null) // case 2)
			updateParents(diff.dataset, add);
	}

	private void updateParents(Dataset dataset, boolean add) {
		String parentId = dataset.getCategoryRefId();
		while (parentId != null) {
			Diff parent = index.get(parentId);
			if (add)
				parent.changedChildren.add(dataset.getRefId());
			else
				parent.changedChildren.remove(dataset.getRefId());
			index.put(parentId, parent);
			parentId = parent.dataset.getCategoryRefId();
		}
		if (add)
			updateChangedTopLevelElements(dataset, DiffType.CHANGED);
		else
			updateChangedTopLevelElements(dataset, DiffType.NO_DIFF);
	}

	public void commit() {
		db.commit();
	}

}
