package org.openlca.core.editors.io;

import java.io.File;

import org.openlca.app.App;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.editors.model.LocalisedImpactMethod;
import org.openlca.core.editors.model.LocalisedMethodBuilder;
import org.openlca.core.model.ImpactMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Localised impact assessment methods are stored in a package format (see
 * {@link LocalisedMethodPackage}) under the folder
 * "localised_methods/<database name>" in the openLCA workspace. The file name
 * has the format "<method id>.llciam".
 */
public class LocalisedMethodStorage {

	public static void save(LocalisedImpactMethod method, IDatabase database) {
		try {
			File file = getFile(database, method.getImpactMethod().getRefId());
			LocalisedMethodPackage methodPackage = new LocalisedMethodPackage(
					file);
			methodPackage.write(method);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(LocalisedMethodStorage.class);
			log.error("Failed to import new impact method", e);
		}
	}

	public static LocalisedImpactMethod getOrCreate(IDatabase database,
			String methodId) {
		File file = LocalisedMethodStorage.getFile(database, methodId);
		if (!file.exists())
			return createNew(database, methodId, file);
		return readMethodFile(file);
	}

	/**
	 * Returns the file for the impact assessment method with the given id and
	 * the given database. It does not create this file, so if the returned file
	 * returns false on file.exists() there is no localised version stored yet.
	 */
	private static File getFile(IDatabase database, String methodId) {
		File workspace = App.getWorkspace();
		File mDir = new File(workspace, "localised_methods");
		File dir = new File(mDir, database.getName());
		if (!dir.exists())
			dir.mkdirs();
		File methodFile = new File(dir, methodId + ".llciam");
		return methodFile;
	}

	private static LocalisedImpactMethod createNew(IDatabase database,
			String methodId, File file) {
		try {
			ImpactMethodDao dao = new ImpactMethodDao(
					database);
			ImpactMethod realMethod = dao.getForRefId(methodId);
			LocalisedMethodBuilder builder = new LocalisedMethodBuilder(
					realMethod, database);
			LocalisedImpactMethod method = builder.build();
			LocalisedMethodPackage pack = new LocalisedMethodPackage(file);
			pack.write(method);
			return method;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(LocalisedMethodStorage.class);
			log.error("Failed to create localised method " + file, e);
			return null;
		}
	}

	private static LocalisedImpactMethod readMethodFile(File file) {
		try {
			LocalisedMethodPackage p = new LocalisedMethodPackage(file);
			return p.read();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(LocalisedMethodStorage.class);
			log.error("Failed to read localised LCIA method file " + file, e);
			return null;
		}
	}

}
