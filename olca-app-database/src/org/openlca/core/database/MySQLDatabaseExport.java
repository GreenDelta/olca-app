package org.openlca.core.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openlca.core.database.util.SQLScriptWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports a database to a zip-file containing the data as sql-insert statements
 * (entry script.sql) and the database schema (entry schema.sql).
 */
class MySQLDatabaseExport {

	private File exportFile;
	private ConnectionData connectionData;
	private Logger log = LoggerFactory.getLogger(getClass());

	public MySQLDatabaseExport(ConnectionData connectionData, File exportFile) {
		this.connectionData = connectionData;
		this.exportFile = exportFile;
	}

	public void run() throws DataProviderException {
		try {
			log.info("Export to file {} ", exportFile);
			File sqlFile = new File(exportFile.getParentFile()
					.getAbsolutePath() + "/script.sql");
			writeFile(sqlFile);

			createZip(sqlFile);
			sqlFile.delete();
		} catch (Exception e) {
			throw new DataProviderException(e);
		}
	}

	private void writeFile(File sqlFile) throws DataProviderException {
		try {
			// connection will be closed in writer.write()
			Connection connection = connectionData.createDatabaseConnection();
			SQLScriptWriter writer = new SQLScriptWriter();
			writer.write(connection, sqlFile);
		} catch (Exception e) {
			throw new DataProviderException(e);
		}
	}

	private void createZip(File sqlFile) throws DataProviderException {
		try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(
				exportFile.getAbsolutePath()))) {
			// fileIn will be closed in writeEntry()
			FileInputStream fileIn = new FileInputStream(sqlFile);
			writeEntry(sqlFile.getName(), fileIn, zipOut);
			writeEntry("schema.sql",
					getClass().getResourceAsStream("current_schema.sql"),
					zipOut);
			byte[] encoding = "UTF-8".getBytes("UTF-8");
			ByteArrayInputStream in = new ByteArrayInputStream(encoding);
			writeEntry("encoding", in, zipOut);
		} catch (Exception e) {
			throw new DataProviderException(e);
		}
	}

	private void writeEntry(String entryName, InputStream in,
			ZipOutputStream zipOut) throws Exception {
		log.trace("pack entry {}", entryName);
		zipOut.putNextEntry(new ZipEntry(entryName));
		int len = -1;
		byte[] buf = new byte[128];
		while ((len = in.read(buf)) > 0) {
			zipOut.write(buf, 0, len);
		}
		in.close();
		zipOut.closeEntry();
	}
}
