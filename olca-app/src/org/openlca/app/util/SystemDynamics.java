package org.openlca.app.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.util.Dirs;

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

	/**
	 * Creates a new model directory under {@code sd-models/{uuid}}.
	 */
	public static Res<File> createModelDir(String uuid, IDatabase db) {
		if (Strings.isBlank(uuid))
			return Res.error("no model ID defined");
		if (db == null)
			return Res.error("no database provided");
		var root = db.getFileStorageLocation();
		if (root == null)
			return Res.error("no file storage available for database");
		var modelRoot = new File(root, "sd-models");
		try {
			Dirs.createIfAbsent(modelRoot);
			var modelDir = new File(modelRoot, uuid);
			Dirs.createIfAbsent(modelDir);
			return Res.ok(modelDir);
		} catch (Exception e) {
			return Res.error("failed to create model folder", e);
		}
	}

	/**
	 * Finds the XMILE file in the given model directory. First looks for
	 * any {@code .xml} file; falls back to {@code model.xml} for legacy
	 * compatibility.
	 */
	public static File getXmileFile(File modelDir) {
		if (modelDir == null || !modelDir.exists())
			return null;
		var files = modelDir.listFiles(
				(dir, name) -> name.endsWith(".xml"));
		if (files != null && files.length > 0)
			return files[0];
		// legacy fallback
		return new File(modelDir, "model.xml");
	}

	/**
	 * Returns the display name for a model directory. This is the name of
	 * the {@code .xml} file without its extension. Falls back to the
	 * directory name if no XML file is found.
	 */
	public static String modelNameOf(File modelDir) {
		var file = getXmileFile(modelDir);
		if (file != null && file.exists()) {
			var name = file.getName();
			return name.endsWith(".xml")
					? name.substring(0, name.length() - 4)
					: name;
		}
		return modelDir != null ? modelDir.getName() : "?";
	}

	/**
	 * Sanitizes a name so it can be used as a file name. Replaces
	 * characters that are not allowed in file names with underscores.
	 */
	public static String sanitizeName(String name) {
		if (Strings.isBlank(name))
			return "System dynamics model";
		return name.strip().replaceAll("[<>:\"/\\\\|?*]", "_");
	}

}
