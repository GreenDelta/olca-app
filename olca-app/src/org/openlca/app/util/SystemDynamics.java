package org.openlca.app.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.openlca.core.database.IDatabase;
import org.openlca.util.Dirs;
import org.openlca.util.Res;
import org.openlca.util.Strings;

public class SystemDynamics {

	private SystemDynamics() {
	}

	public static Optional<File> getModelRootOf(IDatabase db) {
		if (db == null)
			return Optional.empty();
		var dir = db.getFileStorageLocation();
		if (dir == null || !dir.exists())
			return Optional.empty();
		var sdRoot = new File(dir, "sd-models");
		if (!sdRoot.exists() || !sdRoot.isDirectory())
			return Optional.empty();
		return Optional.of(sdRoot);
	}

	public static List<File> getModelDirsOf(IDatabase db) {
		var root = getModelRootOf(db).orElse(null);
		if (root == null)
			return List.of();
		var dirs = root.listFiles();
		return dirs == null || dirs.length == 0
				? List.of()
				: Arrays.asList(dirs);
	}

	public static Res<File> createModelDir(String name, IDatabase db) {
		if (Strings.nullOrEmpty(name))
			return Res.error("no name defined");
		if (db == null)
			return Res.error("no database provided");
		var root = db.getFileStorageLocation();
		if (root == null)
			return Res.error("no file storage available for database");
		var modelRoot = new File(root, "sd-models");
		try {
			Dirs.createIfAbsent(modelRoot);
			var modelDir = new File(modelRoot, name);
			Dirs.createIfAbsent(modelDir);
			return Res.of(modelDir);
		} catch (Exception e) {
			return Res.error("failed to create model folder", e);
		}
	}
}
