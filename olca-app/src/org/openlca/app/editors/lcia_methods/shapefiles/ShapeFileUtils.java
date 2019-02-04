package org.openlca.app.editors.lcia_methods.shapefiles;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swt.SwtMapFrame;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.util.UI;
import org.openlca.core.database.FileStore;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.geo.kml.FeatureType;
import org.openlca.util.Dirs;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.vividsolutions.jts.geom.LineString;

public class ShapeFileUtils {

	/**
	 * Get the folder where the shape-files are stored for the given LCIA,
	 * method.
	 */
	public static File getFolder(ImpactMethod method) {
		if (method == null || method.refId == null)
			return null;
		return DatabaseDir.getDir(method);
	}

	/**
	 * Get the folder where the shape-files are stored for the given LCIA,
	 * method.
	 */
	public static File getFolder(ImpactMethodDescriptor method) {
		if (method == null || method.refId == null)
			return null;
		return new FileStore(Database.get()).getFolder(method);
	}

	/**
	 * Check if the mandatory files that define the shape-file are available
	 * (see http://en.wikipedia.org/wiki/Shapefile).
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
		String[] mandatoryExtensions = { ".shx", ".dbf" };
		for (String ext : mandatoryExtensions) {
			File file = new File(folder, rawName + ext);
			if (!file.exists())
				return false;
		}
		return true;
	}

	/**
	 * Returns true if a shape-file with the given name already exists for the
	 * given method.
	 */
	static boolean alreadyExists(ImpactMethod method, File shapeFile) {
		File folder = getFolder(method);
		if (folder == null || !folder.exists())
			return false;
		File localFile = new File(folder, shapeFile.getName());
		return localFile.exists();
	}

	/**
	 * Imports the given shape-file and the associated files into the folder for
	 * the given impact method.
	 */
	static String importFile(ImpactMethod method, File shapeFile)
			throws Exception {
		File methodFolder = getFolder(method);
		if (!methodFolder.exists())
			methodFolder.mkdirs();
		for (File f : getAllFiles(shapeFile)) {
			File to = new File(methodFolder, f.getName());
			if (f.isFile()) {
				Files.copy(f, to);
			} else {
				Dirs.copy(f.toPath(), to.toPath());
			}
		}
		String shapeFileName = FilenameUtils.removeExtension(shapeFile
				.getName());
		DataStore dataStore = openDataStore(method, shapeFileName);
		Collection<ShapeFileParameter> params = readParameters(dataStore);
		writeParameters(method, shapeFileName, params);
		dataStore.dispose();
		return shapeFileName;
	}

	/**
	 * Get all related files of the given shape-file including the given file
	 * itself and directories.
	 */
	private static List<File> getAllFiles(File shapeFile) {
		if (shapeFile == null || !shapeFile.exists())
			return Collections.emptyList();
		String rawName = FilenameUtils.removeExtension(shapeFile.getName());
		File folder = shapeFile.getParentFile();
		List<File> files = new ArrayList<>();
		for (File file : folder.listFiles()) {
			String fName = FilenameUtils.removeExtension(file.getName());
			if (Strings.nullOrEqual(rawName, fName)) {
				files.add(file);
			}
		}
		return files;
	}

	/**
	 * Returns the names of the shape-files of the given method (without file
	 * extension).
	 */
	public static List<String> getShapeFiles(ImpactMethod method) {
		File folder = getFolder(method);
		return getShapeFiles(folder);
	}

	/**
	 * Returns the names of the shape-files of the given method (without file
	 * extension).
	 */
	public static List<String> getShapeFiles(ImpactMethodDescriptor method) {
		File folder = getFolder(method);
		return getShapeFiles(folder);
	}

	private static List<String> getShapeFiles(File folder) {
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
		for (File file : getAllFiles(shapeFile)) {
			if (file.isDirectory()) {
				Dirs.delete(file.getPath());
			} else {
				boolean b = file.delete();
				if (!b) {
					file.deleteOnExit();
				}
			}
		}
	}

	public static List<ShapeFileParameter> getParameters(ImpactMethod method,
			String shapeFileName) throws IOException {
		File folder = getFolder(method);
		File paramFile = new File(folder, shapeFileName + ".gisolca");
		if (!paramFile.exists())
			return Collections.emptyList();
		try (FileInputStream is = new FileInputStream(paramFile);
				InputStreamReader reader = new InputStreamReader(is, "utf-8");
				BufferedReader buffer = new BufferedReader(reader)) {
			Gson gson = new Gson();
			ShapeFileParameter[] params = gson.fromJson(buffer,
					ShapeFileParameter[].class);
			List<ShapeFileParameter> list = Arrays.asList(params);
			Collections.sort(list, new Comparator<ShapeFileParameter>() {
				@Override
				public int compare(ShapeFileParameter o1,
						ShapeFileParameter o2) {
					return Strings.compare(o1.name, o2.name);
				}
			});
			return list;
		}
	}

	private static void writeParameters(ImpactMethod method,
			String shapeFileName, Collection<ShapeFileParameter> parameters)
			throws IOException {
		File folder = getFolder(method);
		if (!folder.exists())
			folder.mkdirs();
		File paramFile = new File(folder, shapeFileName + ".gisolca");
		try (FileOutputStream os = new FileOutputStream(paramFile);
				OutputStreamWriter writer = new OutputStreamWriter(os, "utf-8");
				BufferedWriter buffer = new BufferedWriter(writer)) {
			Gson gson = new Gson();
			gson.toJson(parameters, buffer);
		}
	}

	static void openFileInMap(ImpactMethod method, String shapeFileName) {
		openFileInMap(method, shapeFileName, null);
	}

	static void openFileInMap(ImpactMethod method, String shapeFileName,
			ShapeFileParameter parameter) {
		DataStore dataStore = openDataStore(method, shapeFileName);
		if (dataStore == null)
			return;
		Logger log = LoggerFactory.getLogger(ShapeFileUtils.class);
		try {
			SimpleFeatureCollection source = dataStore.getFeatureSource(
					dataStore.getTypeNames()[0]).getFeatures();
			SimpleFeatureIterator it = source.features();
			if (it.hasNext()) {
				Object type = it.next().getDefaultGeometry();
				FeatureType fType = FeatureType.POLYGON;
				if (type instanceof com.vividsolutions.jts.geom.Point)
					fType = FeatureType.POINT;
				else if (type instanceof LineString)
					fType = FeatureType.LINE;
				Style style = SLD.createSimpleStyle(source.getSchema());
				if (parameter != null && fType != FeatureType.POINT)
					style = SFMapStyle.create(dataStore, parameter, fType);
				showMapFrame(shapeFileName, source, style);
			}
			it.close();
		} catch (Exception e) {
			log.error("Failed to open shape-file", e);
		} finally {
			dataStore.dispose();
		}
	}

	private static void showMapFrame(String shapeFileName,
			SimpleFeatureCollection source, Style style) {
		MapContent mapContent = new MapContent();
		mapContent.setTitle("Features of " + shapeFileName);
		Layer layer = new FeatureLayer(source, style);
		mapContent.addLayer(layer);
		SwtMapFrame frame = createMapFrame(mapContent);
		frame.setBlockOnOpen(true);
		frame.open();
	}

	private static SwtMapFrame createMapFrame(MapContent mapContent) {
		boolean showMenu = false;
		boolean showToolBar = true;
		boolean showStatusBar = false;
		boolean showLayerTable = false;
		return new SwtMapFrame(showMenu, showToolBar, showStatusBar,
				showLayerTable, mapContent) {
			@Override
			protected Control createContents(Composite parent) {
				Control control = super.createContents(parent);
				Shell shell = getShell();
				Shell parentShell = UI.shell();
				if (shell != null && parentShell != null) {
					Point parentSize = parentShell.getSize();
					shell.setSize((int) (parentSize.x * 0.7),
							(int) (parentSize.y * 0.7));
					UI.center(parentShell, shell);
				}
				return control;
			}
		};
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
			return new ShapefileDataStore(file.toURI().toURL());
		} catch (Exception e) {
			log.error("Failed to open shape-file", e);
			return null;
		}
	}

	private static Collection<ShapeFileParameter> readParameters(
			DataStore dataStore) {
		if (dataStore == null)
			return Collections.emptyList();
		Logger log = LoggerFactory.getLogger(ShapeFileUtils.class);
		try {
			Map<String, ShapeFileParameter> params = new HashMap<>();
			String typeName = dataStore.getTypeNames()[0];
			SimpleFeatureCollection source = dataStore.getFeatureSource(
					typeName).getFeatures();
			SimpleFeatureIterator it = source.features();
			while (it.hasNext()) {
				SimpleFeature feature = it.next();
				readParameters(params, feature);
			}
			it.close();
			return params.values();
		} catch (Exception e) {
			log.error("failed to get parameters from shape file", e);
			return Collections.emptyList();
		}
	}

	private static void readParameters(Map<String, ShapeFileParameter> params,
			SimpleFeature feature) {
		for (Property property : feature.getProperties()) {
			if (!(property.getValue() instanceof Number))
				continue;
			if (property.getName() == null)
				continue;
			String name = property.getName().toString();
			double value = ((Number) property.getValue()).doubleValue();
			ShapeFileParameter param = params.get(name);
			if (param == null) {
				param = new ShapeFileParameter();
				param.name = name;
				param.max = value;
				param.min = value;
				params.put(name, param);
			} else {
				param.max = Math.max(param.max, value);
				param.min = Math.min(param.min, value);
			}
		}
	}
}
