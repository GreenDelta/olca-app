package org.openlca.app.rcp.browser;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.openlca.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserFactory {

	private static Logger log = LoggerFactory.getLogger(BrowserFactory.class);

	private static boolean useXulRunner = false;
	private static boolean initialized = false;

	public static Browser create(Composite parent) {
		if (!initialized)
			initialize();
		Browser browser = null;
		if (useXulRunner) {
			log.trace("Create Mozilla browser");
			browser = createMozilla(parent);
		} else {
			log.trace("Create system browser");
			browser = new Browser(parent, SWT.NONE);
		}
		browser.setJavascriptEnabled(true);
		return browser;
	}

	private static void initialize() {
		log.trace("initialize browser factory");
		if (!Config.useMozilla()) {
			noXulRunner();
			return;
		}
		String xulRunnerPath = Config.getXulRunnerPath();
		if (xulRunnerPath == null) {
			log.trace("No XUL runner found, use system browser");
			noXulRunner();
			return;
		}
		if (OS.getCurrent() != OS.Windows
				|| "x86".equals(System.getProperty("os.arch"))) {
			initializeXulRunner(xulRunnerPath);
			return;
		}
		VsCpp10Check check = new VsCpp10Check();
		check.run();
		if (check.installationFound())
			initializeXulRunner(xulRunnerPath);
		else {
			VsCpp10Message.checkAndShow();
			noXulRunner();
		}
	}

	private static void initializeXulRunner(String xulRunnerPath) {
		try {
			log.trace("Use Mozilla browser, XUL runner found at {}",
					xulRunnerPath);
			System.setProperty("org.eclipse.swt.browser.XULRunnerPath",
					xulRunnerPath);
			initMozillaPrefs();
			useXulRunner = true;
		} catch (Exception e) {
			log.error("Failed to initialize browser factory");
		} finally {
			initialized = true;
		}
	}

	private static void noXulRunner() {
		useXulRunner = false;
		initialized = true;
	}

	private static Browser createMozilla(Composite parent) {
		try {
			return new Browser(parent, SWT.MOZILLA);
		} catch (Throwable e) {
			log.warn(
					"Failed to create Mozilla browser, fall back to system default",
					e);
			useXulRunner = false;
			return new Browser(parent, SWT.NONE);
		}
	}

	private static void initMozillaPrefs() {
		if (OS.getCurrent() != OS.Windows)
			return;
		try {
			File profileDir = getMozillaProfileDir();
			if (profileDir == null)
				return;
			if (!profileDir.exists())
				profileDir.mkdirs();
			File userPrefs = new File(profileDir, "user.js");
			if (userPrefs.exists())
				return;
			log.trace("Copy browser preferences to {}", userPrefs);
			InputStream is = BrowserFactory.class
					.getResourceAsStream("user.js");
			Files.copy(is, userPrefs.toPath());
		} catch (Exception e) {
			log.error("failed to intitialise preferences", e);
		}
	}

	private static File getMozillaProfileDir() {
		String appDirPath = System.getenv("AppData");
		if (appDirPath == null) {
			log.info("could not find system directory %AppData%");
			return null;
		}
		File appDir = new File(appDirPath);
		if (!appDir.exists()) {
			log.info("directory %AppData% {} does not exist", appDir);
			return null;
		}
		return new File(appDir, "Mozilla/eclipse");
	}

}
