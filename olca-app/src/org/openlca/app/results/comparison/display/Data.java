package org.openlca.app.results.comparison.display;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.zeroturnaround.zip.ZipUtil;

class Data {

	static Set<String> loadSkipList(File file) throws IOException {
		Set<String> list = new HashSet<>();
		if (file == null || !file.exists())
			return list;
		for (String line : Files.readAllLines(file.toPath())) {
			if (line.trim().isEmpty())
				continue;
			list.add(line.trim());
		}
		return list;
	}

//	static Set<String> importData(IDatabase db, File file, ModelType type) throws IOException {
//		if (file == null)
//			return new HashSet<>();
//		IDatabase dataDb = new DerbyDatabase(getDbDir(file));
//		if (dataDb.getVersion() > IDatabase.CURRENT_VERSION) {
//			System.out.println("The database is newer than this API supports");
//			System.exit(-1);
//		} else if (dataDb.getVersion() < IDatabase.CURRENT_VERSION) {
//			System.out.println("Import Database : Upgrade");
//			try {
//				Upgrades.on(dataDb);
//				System.out.println("Upgrade ok");
//			} catch (Exception e) {
//				System.out.println("Upgrade ko");
//				e.printStackTrace();
//			}
//		}
//		new DatabaseImport(dataDb, db).run();
//		Set<String> refIds = new HashSet<>();
//		for (CategorizedDescriptor d : Daos.categorized(dataDb, type).getDescriptors()) {
//			refIds.add(d.refId);
//		}
//		return refIds;
//	}

	static File getDbDir(File file) throws IOException {
		if (file.isDirectory()) {
			return file;
		}
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File tempDbFolder = new File(tempDir, UUID.randomUUID().toString());
		tempDbFolder.mkdirs();
		try (InputStream input = new FileInputStream(file)) {
			ZipUtil.unpack(input, tempDbFolder);
		}
		return tempDbFolder;
	}

}
