package org.openlca.app.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.sd.model.SdModel;
import org.openlca.util.Dirs;

public class SystemDynamics {

	private SystemDynamics() {
	}

	public static List<File> getModelDirsOf(IDatabase db) {
		var root = sdRootOf(db).orElse(null);
		if (root == null)
			return List.of();
		var dirs = root.listFiles();
		return dirs == null || dirs.length == 0
				? List.of()
				: Arrays.asList(dirs);
	}

	public static Optional<File> sdRootOf(IDatabase db) {
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

	/// Finds the XMILE file in the given model directory. Looks for any `*.xmile`
	/// or `*.xml` file in that folder.
	public static File getXmileFile(File modelDir) {
		if (modelDir == null || !modelDir.exists()) {
			return null;
		}
		var files = modelDir.listFiles();
		if (files == null ) return null;
		for (var f : files) {
			var name = f.getName();
			if (name.endsWith(".xmile") || name.endsWith(".xml")) {
				return f;
			}
		}
		return null;
	}

	/// Returns the display name for a model directory. This is the name of the
	/// `.xml` file without its extension. Falls back to the  directory name if
	/// no XML file is found.
	public static String modelNameOf(File modelDir) {
		var file = getXmileFile(modelDir);
		if (file != null && file.exists()) {
			var name = file.getName();
			if (name.endsWith(".xmile")) {
				return name.substring(0, name.length() - 6);
			}
			if (name.endsWith(".xml")) {
				return name.substring(0, name.length() - 4);
			}
			return name;
		}
		return modelDir != null ? modelDir.getName() : "?";
	}

	/// Saves the given model to a respective database folder. It returns the
	/// folder (the _model directory_) where the model file is stored.
	public static Res<File> saveModel(SdModel model, IDatabase db) {
		if (model == null || db == null) {
			return Res.error("Provided model or database is null");
		}
		if (Strings.isBlank(model.id())) {
			return Res.error("Model has no UUID set");
		}

		try {
			var dbDir = db.getFileStorageLocation();
			Dirs.createIfAbsent(dbDir);
			var sdRoot = new File(dbDir, "sd-models");
			Dirs.createIfAbsent(sdRoot);
			var dir = new File(sdRoot, model.id());
			Dirs.createIfAbsent(dir);

			var old = getXmileFile(dir);
			var name = sanitize(model.name()) + ".xmile";
			var file = new File(dir, name);
			if (old != null
				&& old.exists()
				&& !old.getName().equals(name)) {
				old.delete();
			}

			var res = model.writeTo(file);
			return res.isError()
				? res.wrapError("Failed to write model to file " + name)
				: Res.ok(dir);
		} catch (Exception e) {
			return Res.error("Failed to store model", e);
		}
	}

	private static String sanitize(String name) {
		return Strings.isBlank(name)
			? "System dynamics model"
			: name.strip().replaceAll("[<>:\"/\\\\|?*]", "_");
	}
}
