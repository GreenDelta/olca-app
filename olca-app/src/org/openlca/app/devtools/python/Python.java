package org.openlca.app.devtools.python;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.core.database.IDatabase;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

class Python {

	static AtomicBoolean folderInitialized = new AtomicBoolean(false);

	private Python() {
	}

	static void exec(String script) {
		File pyDir = new File(App.getWorkspace(), "python");
		if (!folderInitialized.get()) {
			initFolder(pyDir);
			System.setProperty("python.path", pyDir.getAbsolutePath());
			System.setProperty("python.home", pyDir.getAbsolutePath());
			folderInitialized.set(true);
		}
		try {

			// add class bindings from `bindings.properties`
			StringBuilder builder = new StringBuilder();
			Properties properties = new Properties();
			properties.load(Python.class.getResourceAsStream(
					"bindings.properties"));
			properties.forEach((name, fullName) -> {
				builder.append("import ")
						.append(fullName)
						.append(" as ")
						.append(name)
						.append("\n");
			});
			builder.append(script);

			// create the interpreter and execute the script
			String fullScript = builder.toString();
			try (PythonInterpreter py = new PythonInterpreter()) {
				py.set("log", LoggerFactory.getLogger(Python.class));
				IDatabase db = Database.get();
				py.set("db", db);
				py.set("olca", new ScriptApi(db));
				py.set("app", App.class);
				py.set("__datadir__", new File(
						App.getWorkspace(), "script_data").getAbsolutePath());
				py.exec(fullScript);
			}

		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Python.class);
			log.error("failed execute script", e);
		}

	}

	/** Extract the Python library in the workspace folder. */
	private static synchronized void initFolder(File pyDir) {
		try {

			// check if the Python folder is tagged with the
			// current application version
			File versionFile = new File(pyDir, ".version");
			if (versionFile.exists()) {
				byte[] bytes = Files.readAllBytes(versionFile.toPath());
				String v = new String(bytes, "utf-8");
				if (Objects.equals(v, App.getVersion()))
					return;
			}

			// replace it with the current version of the
			// packed Python (Jython) library
			if (pyDir.exists()) {
				FileUtils.deleteDirectory(pyDir);
			}
			pyDir.mkdirs();
			String pyJar = "libs/jython-standalone-2.7.1.jar";
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
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Python.class);
			log.error("failed to initialize Python folder " + pyDir, e);
		}
	}

}
