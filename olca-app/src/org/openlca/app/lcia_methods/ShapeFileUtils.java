package org.openlca.app.lcia_methods;

import com.google.common.io.Files;
import org.apache.commons.io.FilenameUtils;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.core.model.ImpactMethod;
import org.openlca.util.Strings;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ShapeFileUtils {

	/**
	 * Get the folder where the shape-files are stored for the given LCIA,
	 * method.
	 */
	static File getFolder(ImpactMethod method) {
		if (method == null || method.getRefId() == null)
			return null;
		File workspace = App.getWorkspace();
		File dbFolder = new File(workspace, Database.get().getName());
		File shapeFileFolder = new File(dbFolder, "shapefiles");
		File methodFolder = new File(shapeFileFolder, method.getRefId());
		return methodFolder;
	}

	/**
	 * Check if the mandatory files that define the shape-file are
	 * available (see http://en.wikipedia.org/wiki/Shapefile).
	 */
	static boolean isValid(File shapeFile) {
		if (shapeFile == null)
			return false;
		if (!shapeFile.exists())
			return false;
		String fileName = shapeFile.getName();
		if (!FilenameUtils.isExtension(fileName, "shp"))
			return false;
		String rawName = FilenameUtils.removeExtension(fileName);
		File folder = shapeFile.getParentFile();
		String[] mandatoryExtensions = {".shx", ".dbf"};
		for (String ext : mandatoryExtensions) {
			File file = new File(folder, rawName + ext);
			if (!file.exists())
				return false;
		}
		return true;
	}

	/**
	 * Returns true if a shape-file with the given name already exists for
	 * the given method.
	 */
	static boolean alreadyExists(ImpactMethod method, File shapeFile) {
		File folder = getFolder(method);
		if (folder == null || !folder.exists())
			return false;
		File localFile = new File(folder, shapeFile.getName());
		return localFile.exists();
	}

	/**
	 * Imports the given shape-file and the associated files into the folder
	 * for the given impact method.
	 */
	static String importFile(ImpactMethod method, File shapeFile) throws Exception {
		File methodFolder = getFolder(method);
		if (!methodFolder.exists())
			methodFolder.mkdirs();
		List<File> importFiles = getAllFiles(shapeFile);
		for (File importFile : importFiles) {
			File file = new File(methodFolder, importFile.getName());
			Files.copy(importFile, file);
		}
		return FilenameUtils.removeExtension(shapeFile.getName());
	}

	/**
	 * Get all related files of the given shape-file including the given file
	 * itself.
	 */
	private static List<File> getAllFiles(File shapeFile) {
		if (shapeFile == null || !shapeFile.exists())
			return Collections.emptyList();
		String rawName = FilenameUtils.removeExtension(shapeFile.getName());
		File folder = shapeFile.getParentFile();
		List<File> files = new ArrayList<>();
		for (File file : folder.listFiles()) {
			String fName = FilenameUtils.removeExtension(file.getName());
			if (Strings.nullOrEqual(rawName, fName))
				files.add(file);
		}
		return files;
	}

	/**
	 * Returns the names of the shape-files of the given method (without file
	 * extension).
	 */
	static List<String> getShapeFiles(ImpactMethod method) {
		File folder = getFolder(method);
		if (folder == null || !folder.exists())
			return Collections.emptyList();
		List<String> files = new ArrayList<>();
		for (File file : folder.listFiles()) {
			String fileName = file.getName();
			if (FilenameUtils.isExtension(fileName, "shp"))
				files.add(FilenameUtils.removeExtension(fileName));
		}
		Collections.sort(files);
		return files;
	}

	/**
	 * Delete the shape-file with the given name (without file extension) from
	 * the folder of the given LCIA method.
	 */
	static void deleteFile(ImpactMethod method, String shapeFileName) {
		File folder = getFolder(method);
		if(folder == null || !folder.exists())
			return;
		File shapeFile = new File(folder, shapeFileName + ".shp");
		List<File> files = getAllFiles(shapeFile);
		for(File file : files)
			file.delete();
	}
}
