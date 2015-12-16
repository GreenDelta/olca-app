package refdata;

import java.io.File;

import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.io.refdata.RefDataImport;
import org.openlca.util.Dirs;
import org.zeroturnaround.zip.ZipUtil;

public class Main {

	public static void main(String[] args) {
		try {
			System.out.println("  Create database from CSV files");

			System.out.println("  Clean build folders ...");
			Dirs.delete("build");
			Dirs.delete("dist");
			Dirs.make("build");
			Dirs.make("dist");

			System.out.println("  Create empty database ...");
			DerbyDatabase db = new DerbyDatabase(F("build/empty"));
			db.close();
			System.out.println("  done");

			System.out.println("  Create database with units ...");
			db = new DerbyDatabase(F("build/units"));
			RefDataImport refImport = new RefDataImport(F("data/units"), db);
			refImport.run();
			db.close();
			System.out.println("  done");

			System.out.println("  Create database with all data ...");
			db = new DerbyDatabase(F("build/flows"));
			refImport = new RefDataImport(F("data/all"), db);
			refImport.run();
			db.close();
			System.out.println("  done");

			System.out.println("  Package databases ...");
			ZipUtil.pack(F("build/empty"), F("dist/empty.zolca"));
			ZipUtil.pack(F("build/units"), F("dist/units.zolca"));
			ZipUtil.pack(F("build/flows"), F("dist/flows.zolca"));
			System.out.println("  done");

		} catch (Exception e) {
			throw new RuntimeException("Database build failed", e);
		}
	}

	private static File F(String path) {
		return new File(path);
	}

}
