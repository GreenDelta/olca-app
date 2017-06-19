package refdata;

import java.io.File;

import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.io.refdata.RefDataImport;
import org.openlca.util.DQSystems;

public class Main {

	public static void main(String[] args) {
		try {
			System.out.println("  Create database from CSV files");
			Util.clean();
			create("empty", null);
			create("units", "units");
			create("flows", "all");
			Util.zip();
			Util.copyToApp();
			System.out.println("  done");
		} catch (Exception e) {
			throw new RuntimeException("Database build failed", e);
		}
	}

	private static void create(String name, String dataDir) throws Exception {
		System.out.println("  Create " + name + " database ...");
		DerbyDatabase db = new DerbyDatabase(F("build/" + name));
		if (dataDir != null) {
			new RefDataImport(F("data/" + dataDir), db).run();
			if ("all".equals(dataDir)) {
				DQSystems.ecoinvent(db);
			}
		}
		Util.embedUpdates(db);
		db.close();
		System.out.println("  done");
	}

	private static File F(String path) {
		return Util.F(path);
	}

}
