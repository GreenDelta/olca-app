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
import org.openlca.util.Dirs;
import refdata.RefData.Set;

public class Main {

	public static void main(String[] args) {
		try {
			System.out.println("  Create database from CSV files");
			Util.clean();
			create("empty", Set.NONE);
			create("units", Set.UNITS);
			create("flows", Set.FLOWS);
			Util.zip();
			Util.copyToApp();
			System.out.println("  done");
		} catch (Exception e) {
			throw new RuntimeException("Database build failed", e);
		}
	}

	private static void create(String name, Set refSet) throws Exception {
		System.out.println("  Create " + name + " database ...");

		var tempDir = RefData.createTempImportDir(refSet).orElse(null);
		if (tempDir == null) {
			System.out.println("... failed");
			return;
		}

		try (var db = new Derby(F("build/" + name))) {
			new RefDataImport(tempDir, db).run();
			if (refSet == Set.FLOWS) {
				importDQS(db);
				GeoImport.on(db);
			}
		}

		try {
			Dirs.delete(tempDir);
		} catch (Exception e) {
			System.out.println(
					"WARNING: failed to delete temporary folder: " + e);
		}
		System.out.println("  done");
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

	private static File F(String path) {
		return Util.F(path);
	}
}
