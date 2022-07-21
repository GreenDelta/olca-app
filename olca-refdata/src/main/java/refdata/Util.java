package refdata;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.openlca.util.Dirs;
import org.zeroturnaround.zip.ZipUtil;

class Util {

	static void clean() {
		System.out.println("  Clean build folders ...");
		Dirs.delete("download");
		Dirs.delete("build");
		Dirs.delete("dist");
		Dirs.createIfAbsent("download");
		Dirs.createIfAbsent("build");
		Dirs.createIfAbsent("dist");
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

}
