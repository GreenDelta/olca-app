package org.openlca.app.rcp.browser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
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
			useXulRunner = false;
			initialized = true;
			return;
		}
		try {
			String xulRunnerPath = Config.getXulRunnerPath();
			if (xulRunnerPath == null) {
				log.trace("No XUL runner found, use system browser");
				useXulRunner = false;
			} else {
				log.trace("Use Mozilla browser, XUL runner found at {}",
						xulRunnerPath);
				System.setProperty("org.eclipse.swt.browser.XULRunnerPath",
						xulRunnerPath);
				useXulRunner = true;
			}
		} catch (Exception e) {
			log.error("Failed to initialize browser factory");
		} finally {
			initialized = true;
		}
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
		try {

		} catch (Exception e) {

		}
	}

}
