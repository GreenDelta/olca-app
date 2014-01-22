package org.openlca.app.lcia_methods;

import com.google.common.io.Files;
import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.core.model.ImpactMethod;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
		if (folder == null || !folder.exists())
			return;
		File shapeFile = new File(folder, shapeFileName + ".shp");
		List<File> files = getAllFiles(shapeFile);
		for (File file : files)
			file.delete();
	}

	static List<ShapeFileParameter> readParameters(ImpactMethod method, String
			shapeFileName) throws IOException {
		File folder = getFolder(method);
		File paramFile = new File(folder, shapeFileName + ".gisolca");
		if (!paramFile.exists())
			return Collections.emptyList();
		try (FileInputStream is = new FileInputStream(paramFile);
		     InputStreamReader reader = new InputStreamReader(is, "utf-8");
		     BufferedReader buffer = new BufferedReader(reader);
		) {
			Gson gson = new Gson();
			ShapeFileParameter[] params = gson.fromJson(buffer,
					ShapeFileParameter[].class);
			List<ShapeFileParameter> list = Arrays.asList(params);
			Collections.sort(list, new Comparator<ShapeFileParameter>() {
				@Override
				public int compare(ShapeFileParameter o1, ShapeFileParameter o2) {
					return Strings.compare(o1.getName(), o2.getName());
				}
			});
			return list;
		}
	}

	static void writeParameters(ImpactMethod method, String shapeFileName,
	                            List<ShapeFileParameter> parameters)
			throws IOException {
		File folder = getFolder(method);
		if (!folder.exists())
			folder.mkdirs();
		File paramFile = new File(folder, shapeFileName + ".gisolca");
		try (FileOutputStream os = new FileOutputStream(paramFile);
		     OutputStreamWriter writer = new OutputStreamWriter(os, "utf-8");
		     BufferedWriter buffer = new BufferedWriter(writer);
		) {
			Gson gson = new Gson();
			gson.toJson(parameters, buffer);
		}
	}

	static void openFileInMap(ImpactMethod method, String shapeFileName) {
		DataStore dataStore = openDataStore(method, shapeFileName);
		if (dataStore == null)
			return;
		Logger log = LoggerFactory.getLogger(ShapeFileUtils.class);
		try {
			SimpleFeatureCollection source = dataStore.getFeatureSource(
					dataStore.getTypeNames()[0]).getFeatures();
			MapContent mapContent = new MapContent();
			mapContent.setTitle("Features of " + shapeFileName);
			Style style = SLD.createSimpleStyle(source.getSchema());
			Layer layer = new FeatureLayer(source, style);
			mapContent.addLayer(layer);
			JMapFrame.showMap(mapContent);
		} catch (Exception e) {
			log.error("Failed to open shape-file", e);
		}
	}

	static void openFileInMap(ImpactMethod method, String shapeFileName,
	                     String parameter) {
		DataStore dataStore = openDataStore(method, shapeFileName);
		if (dataStore == null)
			return;
		double[] range = getRange(dataStore, parameter);
		Logger log = LoggerFactory.getLogger(ShapeFileUtils.class);
		try {
			SimpleFeatureCollection source = dataStore.getFeatureSource(
					dataStore.getTypeNames()[0]).getFeatures();
			MapContent mapContent = new MapContent();
			mapContent.setTitle("Features of " + shapeFileName);
			Style style = ShapeFileStyle.create(dataStore, parameter, range[0],
					range[1]);
			Layer layer = new FeatureLayer(source, style);
			mapContent.addLayer(layer);
			JMapFrame.showMap(mapContent);
		} catch (Exception e) {
			log.error("Failed to open shape-file", e);
		}
	}

	private static DataStore openDataStore(ImpactMethod method,
	                                       String shapeFileName) {
		File folder = ShapeFileUtils.getFolder(method);
		if (folder == null)
			return null;
		Logger log = LoggerFactory.getLogger(ShapeFileUtils.class);
		try {
			File file = new File(folder, shapeFileName + ".shp");
			log.trace("open shape-file in map: {}", file);
			ShapefileDataStore dataStore = new ShapefileDataStore(
					file.toURI().toURL());
			return dataStore;
		} catch (Exception e) {
			log.error("Failed to open shape-file", e);
			return null;
		}
	}

	private static double[] getRange(DataStore dataStore, String parameter) {
		Logger log = LoggerFactory.getLogger(ShapeFileUtils.class);
		try {

			String typeName = dataStore.getTypeNames()[0];
			SimpleFeatureCollection source = dataStore.getFeatureSource(
					typeName).getFeatures();
			SimpleFeatureIterator it = source.features();
			double min = 0;
			double max = 0;
			while (it.hasNext()) {
				SimpleFeature feature = it.next();
				Object attVal = feature.getAttribute(parameter);
				if(!(attVal instanceof Number))
					continue;
				Number val = (Number) attVal;
				double v = val.doubleValue();
				max = Math.max(max, v);
				min = Math.min(min, v);
			}
			return new double[]{min, max};
		} catch (Exception e) {
			log.error("failed to get parameter range from shape file", e);
			return new double[]{0d, 0d};
		}
	}

}
