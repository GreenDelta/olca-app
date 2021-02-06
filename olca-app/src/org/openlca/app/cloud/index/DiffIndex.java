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
import org.openlca.cloud.model.data.FileReference;
import org.openlca.core.model.ModelType;

// NOT SYNCHRONIZED //
public class DiffIndex {

	private File file;
	private DB db;
	private Map<String, Diff> index;
	private Map<String, Set<String>> changedTopLevelElements;

	public static DiffIndex getFor(RepositoryClient client) {
		RepositoryConfig config = client.getConfig();
		return new DiffIndex(config.getConfigDir());
	}

	private DiffIndex(File indexDirectory) {
		if (!indexDirectory.exists()) {
			indexDirectory.mkdirs();
		}
		file = new File(indexDirectory, "indexfile");
		createDb(file);
	}

	public void init() {
		db.atomicInteger("version").set(2);
	}

	File getDir() {
		if (file == null)
			return null;
		return file.getParentFile();
	}

	private void createDb(File file) {
		db = DBMaker.fileDB(file).lockDisable().closeOnJvmShutdown().make();
		index = db.hashMap("diffIndex");
		changedTopLevelElements = db.hashMap("changedTopLevelElements");
	}

	void open() {
		if (db == null || !db.isClosed())
			return;
		createDb(file);
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
		for (File file : dir.listFiles()) {
			if (file.getName().startsWith("indexfile")) {
				file.delete();
			}
		}
		file = new File(dir, "indexfile");
		createDb(file);
		db.commit();
	}

	public void add(Dataset dataset, long localId) {
		Diff diff = index.get(dataset.toId());
		if (diff != null)
			return;
		diff = new Diff(dataset);
		diff.localId = localId;
		index.put(dataset.toId(), diff);
	}

	public void setTracked(FileReference ref, boolean value) {
		Diff diff = index.get(ref.toId());
		if (diff == null || diff.tracked == value || (!value && diff.type == DiffType.DELETED))
			return;
		diff.tracked = value;
		boolean isChanged = value && diff.changed != null;
		updateParents(diff, isChanged);
		index.put(ref.toId(), diff);
	}

	public void update(Dataset dataset, DiffType newType) {
		Diff diff = index.get(dataset.toId());
		if (diff == null)
			return;
		if (diff.type == DiffType.NEW && newType == DiffType.DELETED) {
			// user added something and then deleted it again
			remove(dataset);
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
		index.put(dataset.toId(), diff);
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
		String parentKey = ModelType.CATEGORY.name() + dataset.categoryRefId;
		while (parentKey != null) {
			Diff parent = index.get(parentKey);
			if (parent == null)
				break;
			if (changed) {
				parent.changedChildren.add(dataset.toId());
			} else {
				parent.changedChildren.remove(dataset.toId());
			}
			index.put(parentKey, parent);
			parentKey = ModelType.CATEGORY.name() + parent.dataset.categoryRefId;
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

	public Diff get(FileReference ref) {
		return ref == null
				? null
				: index.get(ref.toId());
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
				untracked.add(diff.dataset.toId());
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

	public void remove(FileReference ref) {
		Diff diff = index.remove(ref.toId());
		if (diff == null)
			return;
		updateParents(diff, false);
	}

	public void commit() {
		db.commit();
	}

}
