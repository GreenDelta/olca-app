package org.openlca.app.rcp.browser;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.openlca.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Config {

	static boolean useMozilla() {
		if (OS.getCurrent() != OS.Linux && OS.getCurrent() != OS.Windows)
			return false;
		String xulRunnerPath = getXulRunnerPath();
		return xulRunnerPath != null;
	}

	/**
	 * Returns the absolute path to the installed XUL-Runner. Returns null if no
	 * XUL runner installation could be found.
	 */
	static String getXulRunnerPath() {
		Logger log = LoggerFactory.getLogger(Config.class);
		Location location = Platform.getInstallLocation();
		if (location == null)
			return null;
		try {
			URL url = location.getURL();
			File installDir = new File(url.getFile());
			File xulRunnerDir = new File(installDir, "xulrunner");
			log.trace("search for XULRunner at {}", xulRunnerDir);
			if (xulRunnerDir.exists())
				return xulRunnerDir.getAbsolutePath();
			log.trace("no XulRunner found");
			return null;
		} catch (Exception e) {
			log.error("Error while searching for XUL-Runner", e);
			return null;
		}
	}

}
