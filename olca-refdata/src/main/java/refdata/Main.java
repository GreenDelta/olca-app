package refdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.io.refdata.RefDataImport;
import org.openlca.jsonld.MemStore;
import org.openlca.jsonld.input.JsonImport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
				importDQS(db);
			}
		}
		Util.embedUpdates(db);
		db.close();
		System.out.println("  done");
	}

	private static File F(String path) {
		return Util.F(path);
	}

	private static void importDQS(IDatabase db) throws Exception {
		MemStore store = new MemStore();
		String[] dqs = { "ecoinvent_dqs.json", "ilcd_dqs.json" };
		for (String dq : dqs) {
			System.out.println("  ... import DQS " + dq);
			File f = F("data/dqs/" + dq);
			try (InputStream stream = new FileInputStream(f);
					Reader reader = new InputStreamReader(stream, "utf-8");
					BufferedReader buffer = new BufferedReader(reader)) {
				Gson gson = new Gson();
				JsonObject obj = gson.fromJson(buffer, JsonObject.class);
				store.put(ModelType.DQ_SYSTEM, obj);
			}
		}
		JsonImport imp = new JsonImport(store, db);
		imp.run();
	}

}
