package org.openlca.app.preferencepages;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.openlca.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes values from and to the openLCA.ini file which is located in
 * the installation directory of openLCA. Writing is not possible when openLCA
 * is installed in a read-only folder. This class is independent from the user
 * interface and could be re-used in other packages if needed.
 */
class ConfigIniFile {

	private Language language = Language.ENGLISH;
	private int maxMemory = 1500;

	public static ConfigIniFile read() {
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
			if (!iniFile.exists())
				return;
			List<String> oldLines = Files.readAllLines(iniFile.toPath());
			List<String> newLines = new ArrayList<>();
			boolean nextIsLanguage = false;
			for (String line : oldLines) {
				if (line.trim().equals("-nl")) {
					nextIsLanguage = true;
					newLines.add(line);
				} else if (nextIsLanguage) {
					nextIsLanguage = false;
					newLines.add(getLanguage().getCode());
				} else if (line.trim().startsWith("-Xmx")) {
					newLines.add("-Xmx" + maxMemory + "M");
				} else {
					newLines.add(line);
				}
			}
			Files.write(iniFile.toPath(), newLines);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(ConfigIniFile.class);
			log.error("failed to write openLCA.ini file", e);
		}
	}

	private static File getIniFile() {
		Location location = Platform.getInstallLocation();
		URL url = location.getURL();
		File installDir = new File(url.getFile());
		if (OS.getCurrent() != OS.Mac)
			return new File(installDir, "openLCA.ini");
		else
			return new File(installDir, "Contents/MacOS/openLCA.ini");
	}

	private static ConfigIniFile parseFile(File iniFile) throws Exception {
		List<String> lines = Files.readAllLines(iniFile.toPath());
		ConfigIniFile ini = new ConfigIniFile();
		boolean nextIsLanguage = false;
		for (String line : lines) {
			if (line.trim().equals("-nl")) {
				nextIsLanguage = true;
				continue;
			}
			if (nextIsLanguage) {
				ini.language = Language.getForCode(line.trim());
				nextIsLanguage = false;
			} else if (line.trim().startsWith("-Xmx")) {
				readMemory(line, ini);
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

	public Language getLanguage() {
		if (language == null)
			return Language.ENGLISH;
		else
			return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public int getMaxMemory() {
		return maxMemory;
	}

	public void setMaxMemory(int maxMemory) {
		this.maxMemory = maxMemory;
	}

}
