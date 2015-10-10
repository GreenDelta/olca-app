package org.openlca.app.cloud.index;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.greendelta.cloud.model.data.DatasetIdentifier;

// NOT SYNCHRONIZED //
public class DiffIndex {

	private File file;
	private DB db;
	private Map<String, Diff> map;

	public DiffIndex(File indexDirectory) {
		if (!indexDirectory.exists())
			indexDirectory.mkdirs();
		file = new File(indexDirectory, "indexfile");
		createDb(file);
	}

	private void createDb(File file) {
		db = DBMaker.fileDB(file).lockDisable().closeOnJvmShutdown().make();
		map = db.hashMap("diffIndex");
	}

	public void close() {
		if (!db.isClosed())
			db.close();
	}

	void add(DatasetIdentifier identifier) {
		Diff diff = map.get(identifier.getRefId());
		if (diff != null)
			return;
		diff = new Diff(identifier, DiffType.NO_DIFF);
		map.put(identifier.getRefId(), diff);
	}

	void update(DatasetIdentifier identifier, DiffType newType) {
		Diff diff = map.get(identifier.getRefId());
		if (diff.type == DiffType.NEW && newType == DiffType.DELETED) {
			// user added something and then deleted it again
			remove(identifier.getRefId());
			return;
		}
		diff.type = newType;
		if (newType == DiffType.NO_DIFF) {
			updateParents(diff, false);
			diff.identifier = identifier;
			diff.changed = null;
		} else {
			diff.changed = identifier;
			updateParents(diff, true);
		}
		map.put(identifier.getRefId(), diff);
	}

	public Diff get(String key) {
		return map.get(key);
	}

	public List<Diff> getChanged() {
		List<Diff> changed = new ArrayList<>();
		for (Diff diff : map.values())
			if (diff.hasChanged())
				changed.add(diff);
		return changed;
	}

	void remove(String key) {
		Diff diff = map.remove(key);
		updateParents(diff, false);
	}

	private void updateParents(Diff diff, boolean add) {
		if (diff.changed != null) // case 1)
			updateParents(diff.changed, add);
		if (diff.identifier != null) // case 2)
			updateParents(diff.identifier, add);
	}

	private void updateParents(DatasetIdentifier identifier, boolean add) {
		String parentId = identifier.getCategoryRefId();
		while (parentId != null) {
			Diff parent = map.get(parentId);
			if (add)
				parent.changedChildren.add(identifier.getRefId());
			else
				parent.changedChildren.remove(identifier.getRefId());
			map.put(parentId, parent);
			parentId = parent.identifier.getCategoryRefId();
		}
	}

	void commit() {
		db.commit();
	}

}
