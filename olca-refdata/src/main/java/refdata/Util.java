package refdata;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.openlca.core.database.IDatabase;
import org.openlca.updates.UpdateHelper;
import org.openlca.updates.UpdateMetaInfo;
import org.openlca.updates.UpdateMetaInfoStore;
import org.openlca.util.Dirs;
import org.zeroturnaround.zip.ZipUtil;

class Util {
	
	static void clean() {
		System.out.println("  Clean build folders ...");
		Dirs.delete("download");
		Dirs.delete("build");
		Dirs.delete("dist");
		Dirs.make("download");
		Dirs.make("build");
		Dirs.make("dist");
	}
	
	static void zip() {
		System.out.println("  Package databases ...");
		ZipUtil.pack(F("build/empty"), F("dist/empty.zolca"));
		ZipUtil.pack(F("build/units"), F("dist/units.zolca"));
		ZipUtil.pack(F("build/flows"), F("dist/flows.zolca"));
	}
	
	static void copyToApp() throws Exception {
		File appDir = F("../olca-app/db_templates");
		if (!appDir.exists()) {
			System.out.println("  WARNING: ../olca-app/db_templates does not exist");
			return;
		}
		System.out.println("  Copy to app ...");
		String[] dbs = { "empty.zolca", "units.zolca", "flows.zolca" };
		for (String db : dbs) {
			File src = F("dist/" + db);
			File target = new File(appDir, db);
			Files.copy(src.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	static File F(String path) {
		return new File(path);
	}

	static void embedUpdates(IDatabase db) {
		UpdateMetaInfoStore store = new UpdateMetaInfoStore(db);
		UpdateHelper helper = new UpdateHelper(db, null, null);
		Set<UpdateMetaInfo> all = helper.getAllUpdates();
		for (UpdateMetaInfo m : all) {
			m.executed = true;
			store.save(m);
		}
	}
}
