package org.openlca.app.db;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.io.olca.DatabaseImport;
import org.zeroturnaround.zip.ZipUtil;

public class MySQLDatabaseExport implements Runnable {

	private MySQLConfiguration config;
	private File zolcaFile;
	private boolean success = false;

	public MySQLDatabaseExport(MySQLConfiguration config, File zolcaFile) {
		this.config = config;
		this.zolcaFile = zolcaFile;
	}

	public boolean doneWithSuccess() {
		return success;
	}

	@Override
	public void run() {
		try {
			IDatabase sourceDb = config.connect();
			DerbyDatabase targetDb = createTemporaryDb();
			DatabaseImport io = new DatabaseImport(sourceDb, targetDb);
			io.run();
			sourceDb.close();
			targetDb.close();
			ZipUtil.pack(targetDb.getDatabaseDirectory(), zolcaFile);
			FileUtils.deleteDirectory(targetDb.getDatabaseDirectory());
			success = true;
		} catch (Exception e) {
			success = false;
			ErrorReporter.on("failed export MySQL database as zolca-File", e);
		}
	}

	private DerbyDatabase createTemporaryDb() {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		String dbName = "olca_tempdb_"
				+ UUID.randomUUID().toString().replace("-", "");
		File dbDir = new File(tempDir, dbName);
		return new DerbyDatabase(dbDir);
	}

}
