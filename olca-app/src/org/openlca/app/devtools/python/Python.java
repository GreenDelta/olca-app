package org.openlca.app.devtools.python;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.RcpActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

public class Python {

	private static boolean initialized = false;
	private static org.openlca.updates.script.Python python;

	public static void eval(String script) {
		try {
			if (!initialized)
				initialize();
			python.setDatabase(Database.get());
			python.setContext(App.getCalculationContext());
			python.eval(script);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Python.class);
			log.error("failed to evaluate script", e);
		}
	}

	public static File getDir() {
		if (!initialized)
			initialize();
		File workspace = App.getWorkspace();
		return new File(workspace, "python");
	}

	private static synchronized void initialize() {
		if (initialized)
			return;
		Logger log = LoggerFactory.getLogger(Python.class);
		File workspace = App.getWorkspace();
		File pyDir = new File(workspace, "python");
		log.info("initialize Python interpreter");
		try {
			if (!matchVersion(pyDir))
				initPythonDir(pyDir);
			python = new org.openlca.updates.script.Python(pyDir);
			python.register("app", App.class);
			File dataDir = new File(workspace, "script_data");
			if (!dataDir.exists())
				dataDir.mkdir();
			python.setDataDir(dataDir);
			initialized = true;
		} catch (Exception e) {
			log.error("failed to initialize python interpreter @" + pyDir, e);
		}
	}

	private static boolean matchVersion(File pyDir) throws Exception {
		File versionFile = new File(pyDir, ".version");
		if (!versionFile.exists())
			return false;
		byte[] bytes = Files.readAllBytes(versionFile.toPath());
		String v = new String(bytes, "utf-8");
		return Objects.equals(v, App.getVersion());
	}

	private static void initPythonDir(File pyDir) throws Exception {
		if (pyDir.exists())
			FileUtils.deleteDirectory(pyDir);
		pyDir.mkdirs();
		String pyJar = "libs/jython-standalone-2.7.0.jar";
		try (InputStream is = RcpActivator.getStream(pyJar)) {
			ZipUtil.unpack(is, pyDir, (entry) -> {
				if (entry.startsWith("Lib/") && entry.length() > 4) {
					return entry.substring(4);
				} else {
					return null;
				}
			});
		}
		File file = new File(pyDir, ".version");
		Files.write(file.toPath(), App.getVersion().getBytes("utf-8"));
	}
}
