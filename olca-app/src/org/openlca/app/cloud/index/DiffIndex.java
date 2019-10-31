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
		diff = new Diff(dataset);
		diff.localId = localId;
		index.put(dataset.refId, diff);
	}

	public void setTracked(String refId, boolean value) {
		Diff diff = index.get(refId);
		if (diff == null || diff.tracked == value || (!value && diff.type == DiffType.DELETED))
			return;
		diff.tracked = value;
		boolean isChanged = value && diff.changed != null;
		updateParents(diff, isChanged);
		index.put(refId, diff);
	}

	public void update(Dataset dataset, DiffType newType) {
		Diff diff = index.get(dataset.refId);
		if (diff == null)
			return;
		if (diff.type == DiffType.NEW && newType == DiffType.DELETED) {
			// user added something and then deleted it again
			remove(dataset.refId);
			updateParents(diff, false);
			return;
		}
		updateDiff(diff, dataset, newType);
	}

	private void updateDiff(Diff diff, Dataset dataset, DiffType newType) {
		boolean changed = newType != DiffType.NO_DIFF;
		diff.type = newType;
		if (newType == DiffType.NO_DIFF) {
			diff.dataset = dataset;
			diff.changed = null;
		} else {
			diff.changed = dataset;
		}
		updateParents(diff, changed);
		index.put(dataset.refId, diff);
	}

	private void updateParents(Diff diff, boolean changed) {
		if (diff.changed != null) {
			updateParents(diff.changed, changed);
		}
		if (diff.dataset != null) {
			updateParents(diff.dataset, changed);
		}
	}

	private void updateParents(Dataset dataset, boolean changed) {
		String parentId = dataset.categoryRefId;
		while (parentId != null) {
			Diff parent = index.get(parentId);
			if (parent == null)
				break;
			if (changed) {
				parent.changedChildren.add(dataset.refId);
			} else {
				parent.changedChildren.remove(dataset.refId);
			}
			index.put(parentId, parent);
			parentId = parent.dataset.categoryRefId;
		}
		ModelType categoryType = dataset.type == ModelType.CATEGORY ? dataset.categoryType : dataset.type;
		updateChangedTopLevelElements(categoryType.name(), dataset.refId, changed);
	}

	private void updateChangedTopLevelElements(String type, String refId, boolean changed) {
		Set<String> elements = changedTopLevelElements.get(type);
		if (elements == null) {
			elements = new HashSet<>();
		}
		if (changed) {
			elements.add(refId);
		} else {
			elements.remove(refId);
		}
		changedTopLevelElements.put(type, elements);
	}

	public Diff get(String key) {
		return index.get(key);
	}

	public List<Diff> getChanged() {
		List<Diff> changed = new ArrayList<>();
		for (Diff diff : index.values()) {
			if (diff.hasChanged()) {
				changed.add(diff);
			}
		}
		return changed;
	}

	public List<String> getUntracked() {
		List<String> untracked = new ArrayList<>();
		for (Diff diff : index.values()) {
			if (!diff.tracked) {
				untracked.add(diff.dataset.refId);
			}
		}
		return untracked;
	}

	public List<Diff> getAll(DiffType... types) {
		if (types == null || types.length == 0)
			return new ArrayList<>(index.values());
		List<Diff> ofType = new ArrayList<>();
		for (Diff diff : index.values()) {
			for (DiffType type : types) {
				if (diff.type == type) {
					ofType.add(diff);
				}
			}
		}
		return ofType;
	}

	public boolean hasChanged(ModelType type) {
		Set<String> elements = changedTopLevelElements.get(type.name());
		return elements != null && !elements.isEmpty();
	}

	public void remove(String key) {
		Diff diff = index.remove(key);
		if (diff == null)
			return;
		updateParents(diff, false);
	}

	public void commit() {
		db.commit();
	}

}
