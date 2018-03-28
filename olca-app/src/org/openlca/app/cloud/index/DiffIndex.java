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
import org.openlca.util.Dirs;

// NOT SYNCHRONIZED //
public class DiffIndex {

	private File file;
	private DB db;
	private Map<String, Diff> index;
	private Map<String, Set<String>> changedTopLevelElements;

	public static DiffIndex getFor(RepositoryClient client) {
		RepositoryConfig config = client.getConfig();
		return new DiffIndex(getIndexFile(config));
	}

	public static File getIndexFile(RepositoryConfig config) {
		return new File(config.database.getFileStorageLocation(), "cloud/" + config.repositoryId);
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
		if (db.isClosed())
			return;
		db.commit();
		db.close();
	}

	public void clear() {
		close();
		File dir = file.getParentFile();
		Dirs.delete(dir.toPath());
		dir.mkdirs();
		file = new File(dir, "indexfile");
		createDb(file);
		db.commit();
	}

	public void add(Dataset dataset, long localId) {
		Diff diff = index.get(dataset.refId);
		if (diff != null)
			return;
		diff = new Diff(dataset, DiffType.NO_DIFF);
		diff.localId = localId;
		index.put(dataset.refId, diff);
	}

	public void update(Dataset dataset, DiffType newType) {
		Diff diff = index.get(dataset.refId);
		if (diff == null)
			return;
		if (diff.type == DiffType.DELETED && newType == DiffType.DELETED)
			return;
		if (diff.type == DiffType.NEW && newType == DiffType.DELETED) {
			// user added something and then deleted it again
			remove(dataset.refId);
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
		if (dataset.categoryRefId == null) {
			ModelType type = dataset.type;
			if (type == ModelType.CATEGORY) {
				type = dataset.categoryType;
			}
			updateChangedTopLevelElements(type.name(), dataset.refId, newType);
		}
		index.put(dataset.refId, diff);
	}

	private void updateChangedTopLevelElements(String type, String refId, DiffType newType) {
		Set<String> elements = changedTopLevelElements.get(type);
		if (elements == null)
			elements = new HashSet<>();
		if (newType == DiffType.NO_DIFF)
			elements.remove(refId);
		else
			elements.add(refId);
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

	public List<Diff> getAll() {
		return new ArrayList<>(index.values());
	}

	public boolean hasChanged(ModelType type) {
		Set<String> elements = changedTopLevelElements.get(type.name());
		return elements != null && !elements.isEmpty();
	}

	public void remove(String key) {
		Diff diff = index.remove(key);
		if (diff == null)
			return;
		Dataset ds = diff.getDataset();
		updateChangedTopLevelElements(ds.categoryType.name(), ds.refId, DiffType.NO_DIFF);
		updateParents(diff, false);
	}

	private void updateParents(Diff diff, boolean add) {
		if (diff.changed != null) // case 1)
			updateParents(diff.changed, add);
		if (diff.dataset != null) // case 2)
			updateParents(diff.dataset, add);
	}

	private void updateParents(Dataset dataset, boolean add) {
		String parentId = dataset.categoryRefId;
		while (parentId != null) {
			Diff parent = index.get(parentId);
			if (parent == null)
				break;
			if (add)
				parent.changedChildren.add(dataset.refId);
			else
				parent.changedChildren.remove(dataset.refId);
			index.put(parentId, parent);
			parentId = parent.dataset.categoryRefId;
		}
		ModelType categoryType = dataset.categoryType;
		if (categoryType == null)
			categoryType = dataset.type;
		if (add)
			updateChangedTopLevelElements(categoryType.name(), dataset.refId, DiffType.CHANGED);
		else
			updateChangedTopLevelElements(categoryType.name(), dataset.refId, DiffType.NO_DIFF);
	}

	public void commit() {
		db.commit();
	}

}
