package refdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.openlca.core.database.Derby;
import org.openlca.core.database.IDatabase;
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
		var db = new Derby(F("build/" + name));
		if (dataDir != null) {
			new RefDataImport(F("data/" + dataDir), db).run();
			if ("all".equals(dataDir)) {
				importDQS(db);
				GeoImport.on(db);
			}
		}
		db.close();
		System.out.println("  done");
	}

	private static File F(String path) {
		return Util.F(path);
	}

	private static void importDQS(IDatabase db) throws Exception {
		var store = new MemStore();
		String[] dqs = {"ecoinvent_dqs.json", "ilcd_dqs.json"};
		for (String dq : dqs) {
			System.out.println("  ... import DQS " + dq);
			File f = F("data/dqs/" + dq);
			try (var stream = new FileInputStream(f);
				 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
				 var buffer = new BufferedReader(reader)) {
				var obj = new Gson().fromJson(buffer, JsonObject.class);
				store.put(ModelType.DQ_SYSTEM, obj);
			}
		}
		new JsonImport(store, db).run();
	}

}
