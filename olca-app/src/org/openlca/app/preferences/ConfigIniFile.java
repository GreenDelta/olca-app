package org.openlca.app.preferences;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.util.MsgBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes values from and to the openLCA.ini file which is located in
 * the installation directory of openLCA. Writing is not possible when openLCA
 * is installed in a read-only folder.
 */
class ConfigIniFile {

	private static final String EDGE_PROP = "-Dorg.eclipse.swt.browser.DefaultType=edge";

	/// The memory settings of the JVM. These are not configurable by the user
	/// anymore. The heap starts with 256 MB, tries to stay below 4 GB, but can
	/// grow up to 75% of the physical memory if required. Unused memory is
	/// returned to the operating system after 60 seconds.
	private static final List<String> MEMORY_ARGS = List.of(
			"-XX:+UseZGC",
			"-Xms256m",
			"-XX:SoftMaxHeapSize=4g",
			"-XX:MaxRAMPercentage=75",
			"-XX:ZUncommitDelay=60");

	private Language language = Language.ENGLISH;
	private Theme theme = Theme.DEFAULT;
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

	// region: accessors
	Language language() {
		return language == null
				? Language.ENGLISH
				: language;
	}

	void language(Language language) {
		this.language = language;
	}

	void theme(Theme theme) {
		this.theme = theme;
	}

	public Theme theme() {
		return theme == null
				? Theme.DEFAULT
				: theme;
	}

	void useEdgeBrowser(boolean useEdge) {
		this.useEdgeBrowser = useEdge;
	}

	boolean useEdgeBrowser() {
		return useEdgeBrowser;
	}

	// endregion

	public void write() {
		try {
			var iniFile = getIniFile();
			if (!iniFile.exists()) {
				MsgBox.error(M.CouldNotFindIniFile,
						M.IniFileDoesNotExist + " - " + iniFile);
				return;
			}

			var oldLines = Files.readAllLines(iniFile.toPath());
			var newLines = new ArrayList<>(List.of(
					"-nl", language.getCode(),
					"-theme", theme().getCode()
			));
			// Note: Platform-specific dark CSS files are now loaded via the 
			// org.eclipse.e4.ui.css.swt.theme extension point in plugin.xml
			// instead of using -applicationCSS, which ensures proper loading order and OS-specific application

			for (int i = 0; i < oldLines.size(); i++) {

				var line = oldLines.get(i).strip();

				if (line.equals("-nl")
						|| line.equals("-theme")
						|| line.equals("-applicationCSS")) {
					i++;
					continue;
				}

				// memory settings; we remove old settings (e.g. -Xmx from
				// previous versions) and add the current ones after -vmargs
				if (isMemoryArg(line)) {
					continue;
				}

				// Edge browser
				if (line.equals(EDGE_PROP)) {
					continue;
				}

				newLines.add(line);
				if (line.equals("-vmargs")) {
					newLines.addAll(MEMORY_ARGS);
				}
			}

			if (!newLines.contains("-vmargs")) {
				newLines.add("-vmargs");
				newLines.addAll(MEMORY_ARGS);
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
		var dir = App.getInstallLocation();
		return new File(dir, "openLCA.ini");
	}

	private static ConfigIniFile parseFile(File iniFile) throws Exception {
		var lines = Files.readAllLines(iniFile.toPath());

		var ini = new ConfigIniFile();
		boolean nextIsLanguage = false;
		boolean nextIsTheme = false;

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

			// read theme code
			if (line.equals("-theme")) {
				nextIsTheme = true;
				continue;
			}
			if (nextIsTheme) {
				ini.theme = Theme.getForCode(line);
				nextIsTheme = false;
				continue;
			}

			// edge browser
			if (line.equals(EDGE_PROP)) {
				ini.useEdgeBrowser = true;
			}
		}
		return ini;
	}

	/// Returns `true` if the given line is a JVM argument that is related to the
	/// heap size or garbage collector. These lines are replaced by the fixed
	/// memory settings when the ini file is written.
	private static boolean isMemoryArg(String line) {
		if (line == null)
			return false;
		var arg = line.strip();
		return arg.startsWith("-Xmx")
				|| arg.startsWith("-Xms")
				|| arg.startsWith("-XX:SoftMaxHeapSize=")
				|| arg.startsWith("-XX:MaxRAMPercentage=")
				|| arg.startsWith("-XX:ZUncommitDelay=")
				|| arg.equals("-XX:+UseZGC");
	}


}
