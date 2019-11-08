package org.openlca.app.cloud.index;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.mapdb.DB;
import org.mapdb.DBMaker;

public class DiffIndexUpgrades {

	public static final int CURRENT_VERSION = 2;

	public static int getVersion(DiffIndex index) {
		index.close();
		File file = new File(index.getDir(), "indexfile");
		DB db = DBMaker.fileDB(file).lockDisable().closeOnJvmShutdown().make();
		org.mapdb.Atomic.Integer v = db.atomicInteger("version");
		int version = v == null || v.intValue() <= 0 ? 1 : v.intValue();
		db.close();
		index.open();
		return version;
	}

	public static void upgradeFrom(DiffIndex index, int version) {
		if (version == CURRENT_VERSION)
			return;
		if (version < 2) {
			upgradeV2(index);
		}
	}

	private static void upgradeV2(DiffIndex index) {
		index.close();
		File file = new File(index.getDir(), "indexfile");
		DB db = DBMaker.fileDB(file).lockDisable().closeOnJvmShutdown().make();
		org.mapdb.Atomic.Integer v = db.atomicInteger("version");
		File tmpFile = new File(index.getDir(), "tmpindexfile");
		DB db2 = DBMaker.fileDB(tmpFile).lockDisable().closeOnJvmShutdown().make();
		Map<String, Diff> oldIndex = db.hashMap("diffIndex");
		Map<String, Set<String>> changedTopLevelElements = db.hashMap("changedTopLevelElements");
		Map<String, Diff> tmpIndex = db2.hashMap("diffIndex");
		Map<String, Set<String>> tmpChangedTopLevelElements = db2.hashMap("changedTopLevelElements");
		for (String oldKey : oldIndex.keySet()) {
			Diff value = oldIndex.get(oldKey);
			tmpIndex.put(value.getDataset().toId(), value);
		}
		tmpChangedTopLevelElements.putAll(changedTopLevelElements);
		v = db2.atomicInteger("version");
		v.set(2);
		db2.commit();
		db2.close();
		db.close();
		file.delete();
		tmpFile.renameTo(file);
		index.open();
	}

}
