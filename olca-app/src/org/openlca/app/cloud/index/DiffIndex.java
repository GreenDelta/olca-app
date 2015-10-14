package org.openlca.app.cloud.index;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.greendelta.cloud.model.data.DatasetDescriptor;

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

	void add(DatasetDescriptor descriptor) {
		Diff diff = map.get(descriptor.getRefId());
		if (diff != null)
			return;
		diff = new Diff(descriptor, DiffType.NO_DIFF);
		map.put(descriptor.getRefId(), diff);
	}

	void update(DatasetDescriptor descriptor, DiffType newType) {
		Diff diff = map.get(descriptor.getRefId());
		if (diff.type == DiffType.NEW && newType == DiffType.DELETED) {
			// user added something and then deleted it again
			remove(descriptor.getRefId());
			return;
		}
		diff.type = newType;
		if (newType == DiffType.NO_DIFF) {
			updateParents(diff, false);
			diff.descriptor = descriptor;
			diff.changed = null;
		} else {
			diff.changed = descriptor;
			updateParents(diff, true);
		}
		map.put(descriptor.getRefId(), diff);
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
		if (diff.descriptor != null) // case 2)
			updateParents(diff.descriptor, add);
	}

	private void updateParents(DatasetDescriptor descriptor, boolean add) {
		String parentId = descriptor.getCategoryRefId();
		while (parentId != null) {
			Diff parent = map.get(parentId);
			if (add)
				parent.changedChildren.add(descriptor.getRefId());
			else
				parent.changedChildren.remove(descriptor.getRefId());
			map.put(parentId, parent);
			parentId = parent.descriptor.getCategoryRefId();
		}
	}

	void commit() {
		db.commit();
	}

}
