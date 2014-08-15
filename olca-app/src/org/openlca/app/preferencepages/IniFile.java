package org.openlca.app.preferencepages;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IniFile {

	private Language language = Language.ENGLISH;

	public static IniFile read() {
		try {
			File iniFile = getIniFile();
			if (!iniFile.exists())
				return new IniFile();
			return parseFile(iniFile);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(IniFile.class);
			log.error("failed to read openLCA.ini file", e);
			return new IniFile();
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
				} else {
					newLines.add(line);
				}
			}
			Files.write(iniFile.toPath(), newLines);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(IniFile.class);
			log.error("failed to write openLCA.ini file", e);
		}
	}

	private static File getIniFile() {
		Location location = Platform.getInstallLocation();
		URL url = location.getURL();
		File installDir = new File(url.getFile());
		File iniFile = new File(installDir, "openLCA.ini");
		return iniFile;
	}

	private static IniFile parseFile(File iniFile) throws Exception {
		List<String> lines = Files.readAllLines(iniFile.toPath());
		IniFile ini = new IniFile();
		boolean nextIsLanguage = false;
		for (String line : lines) {
			if (line.trim().equals("-nl")) {
				nextIsLanguage = true;
				continue;
			}
			if (nextIsLanguage) {
				ini.language = Language.getForCode(line.trim());
				nextIsLanguage = false;
			}
		}
		return ini;
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

}
