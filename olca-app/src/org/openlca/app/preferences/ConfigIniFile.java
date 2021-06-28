package org.openlca.app.preferences;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openlca.app.App;
import org.openlca.app.util.MsgBox;
import org.openlca.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes values from and to the openLCA.ini file which is located in
 * the installation directory of openLCA. Writing is not possible when openLCA
 * is installed in a read-only folder.
 */
class ConfigIniFile {

	private static final String EDGE_PROP = "-Dorg.eclipse.swt.browser.DefaultType=edge";

	private Language language = Language.ENGLISH;
	private int maxMemory = 3584;
	private boolean useEdgeBrowser = false;

	static ConfigIniFile read() {
		try {
			File iniFile = getIniFile();
			if (!iniFile.exists())
				return new ConfigIniFile();
			return parseFile(iniFile);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(ConfigIniFile.class);
			log.error("failed to read openLCA.ini file", e);
			return new ConfigIniFile();
		}
	}

	public void write() {
		try {
			File iniFile = getIniFile();
			if (!iniFile.exists()) {
				MsgBox.error("Could not find *.ini file",
						iniFile + " does not exist.");
				return;
			}

			var oldLines = Files.readAllLines(iniFile.toPath());
			var newLines = new ArrayList<String>();
			boolean nextIsLanguage = false;

			for (var l : oldLines) {
				var line = l.trim();

				// application language
				if (line.equals("-nl")) {
					nextIsLanguage = true;
					newLines.add(line);
					continue;
				}
				if (nextIsLanguage) {
					nextIsLanguage = false;
					newLines.add(getLanguage().getCode());
					continue;
				}

				// memory
				if (line.trim().startsWith("-Xmx")) {
					newLines.add("-Xmx" + maxMemory + "M");
					continue;
				}

				// Edge browser
				if (line.equals(EDGE_PROP)) {
					continue;
				}

				newLines.add(line);
			}

			if (useEdgeBrowser) {
				newLines.add(EDGE_PROP);
			}

			Files.write(iniFile.toPath(), newLines);

		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(ConfigIniFile.class);
			log.error("failed to write openLCA.ini file", e);
		}
	}

	private static File getIniFile() {
		File dir = App.getInstallLocation();
		if (OS.get() == OS.MAC)
			return new File(dir, "eclipse.ini");
		else
			return new File(dir, "openLCA.ini");
	}

	private static ConfigIniFile parseFile(File iniFile) throws Exception {
		var lines = Files.readAllLines(iniFile.toPath());

		var ini = new ConfigIniFile();
		boolean nextIsLanguage = false;

		for (var l : lines) {
			var line = l.trim();

			// read language code
			if (line.equals("-nl")) {
				nextIsLanguage = true;
				continue;
			}
			if (nextIsLanguage) {
				ini.language = Language.getForCode(line);
				nextIsLanguage = false;
				continue;
			}

			// memory
			if (line.startsWith("-Xmx")) {
				readMemory(line, ini);
				continue;
			}

			// edge browser
			if (line.equals(EDGE_PROP)) {
				ini.useEdgeBrowser = true;
			}
		}
		return ini;
	}

	private static void readMemory(String line, ConfigIniFile ini) {
		if (line == null || ini == null)
			return;
		String memStr = line.trim().toLowerCase();
		Pattern pattern = Pattern.compile("-xmx([0-9]+)m");
		Matcher matcher = pattern.matcher(memStr);
		if (!matcher.find()) {
			Logger log = LoggerFactory.getLogger(ConfigIniFile.class);
			log.warn("could not extract memory value from "
					+ "{} with -xmx([0-9]+)m", memStr);
			return;
		}
		try {
			int val = Integer.parseInt(matcher.group(1));
			ini.setMaxMemory(val);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(ConfigIniFile.class);
			log.error("failed to parse memory value from ini: " + memStr, e);
		}
	}

	Language getLanguage() {
		if (language == null)
			return Language.ENGLISH;
		else
			return language;
	}

	void setLanguage(Language language) {
		this.language = language;
	}

	int getMaxMemory() {
		return maxMemory;
	}

	void setMaxMemory(int maxMemory) {
		this.maxMemory = maxMemory;
	}

	void setUseEdgeBrowser(boolean useEdge) {
		this.useEdgeBrowser = useEdge;
	}

	boolean useEdgeBrowser() {
		return useEdgeBrowser;
	}

}
