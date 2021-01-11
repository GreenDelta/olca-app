package refdata;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.openlca.core.database.IDatabase;
import org.openlca.geo.GeoJsonImport;

/**
 * Import the ecoinvent geography shapes into a database. It downloads them
 * from https://geography.ecoinvent.org/ if they are not yet available in the
 * "target" directory of this project and imports the using the standard
 * openLCA GeoJSON import.
 */
class GeoImport {

	private final IDatabase db;

	private GeoImport(IDatabase db) {
		this.db = db;
	}

	static void on(IDatabase db) {
		new GeoImport(db).run();
	}

	private void run() {
		var file = getFile();
		if (file == null)
			return;
		System.out.println("  ... import ei3 geographies ...");
		new GeoJsonImport(file, db).run();
	}

	private File getFile() {
		var file = new File("target/geographies.geojson");
		if (file.exists() && file.isFile())
			return file;
		try {
			var path = "https://geography.ecoinvent.org/files/all.geojson.bz2";
			System.out.println("  ... fetch geographies from: " + path + " ...");
			var url = new URL(path);
			try (var bz2 = url.openStream();
				var input = new BZip2CompressorInputStream(bz2)) {
				Files.copy(input, file.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			}
			return file;
		} catch (Exception e) {
			System.out.println(
					"Failed to download and extract ei3 locations: "
					+ e.getMessage());
			return null;
		}
	}
}
