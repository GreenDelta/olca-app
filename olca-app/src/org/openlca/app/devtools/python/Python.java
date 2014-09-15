package org.openlca.app.devtools.python;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.ScriptApi;
import org.openlca.app.rcp.RcpActivator;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

class Python {

	private static final String version = "2.7-b3";

	private static boolean initialized = false;

	public static void eval(String script) {
		try {
			if (!initialized)
				initialize();
			doEval(script);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Python.class);
			log.error("failed to evaluate script", e);
		}
	}

	private static void doEval(String script) {
		try (PythonInterpreter interpreter = new PythonInterpreter()) {
			interpreter.set("log", LoggerFactory.getLogger(Python.class));
			if (Database.get() != null)
				interpreter.set("db", Database.get());
			ScriptApi api = new ScriptApi(Database.get());
			interpreter.set("olca", api);
			interpreter.set("app", App.class);
			interpreter.exec(script);
		}
	}

	private static void initialize() throws Exception {
		File workspace = App.getWorkspace();
		File pyDir = new File(workspace, "python");
		if (!matchVersion(pyDir))
			initPythonDir(pyDir);
		System.setProperty("python.path", pyDir.getAbsolutePath());
		System.setProperty("python.home", pyDir.getAbsolutePath());
		initialized = true;
	}

	private static boolean matchVersion(File pyDir) throws Exception {
		File versionFile = new File(pyDir, ".version");
		if (!versionFile.exists())
			return false;
		byte[] bytes = Files.readAllBytes(versionFile.toPath());
		String v = new String(bytes, "utf-8");
		return Objects.equals(v, version);
	}

	private static void initPythonDir(File pyDir) throws Exception {
		if (pyDir.exists())
			FileUtils.deleteDirectory(pyDir);
		pyDir.mkdirs();
		String pyJar = "libs/jython-standalone-2.7-b3.jar";
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
		Files.write(file.toPath(), version.getBytes("utf-8"));
	}
}
