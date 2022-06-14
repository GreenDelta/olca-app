package org.openlca.app.editors.graphical_legacy.themes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openlca.app.rcp.Workspace;
import org.slf4j.LoggerFactory;

public final class Themes {

	private Themes() {
	}

	public static Theme getDefault() {
		return loadFromWorkspace()
			.stream()
			.filter(theme -> "Default".equalsIgnoreCase(theme.name()))
			.findAny()
			.orElse(noCss().get(0));
	}

	public static Theme get(String file) {
		return loadFromWorkspace()
				.stream()
				.filter(theme -> Objects.equals(file, theme.file()))
				.findAny()
				.orElse(noCss().get(0));
	}

	public static List<Theme> loadFromWorkspace() {
		var dir = new File(Workspace.root(), "graph-themes");
		return loadFrom(dir);
	}

	public static synchronized List<Theme> loadFrom(File dir) {

		// create the directory if it does not exist
		if (dir == null)
			return noCss();
		if (!dir.exists()) {
			try {
				Files.createDirectories(dir.toPath());
			} catch (Exception e) {
				var log = LoggerFactory.getLogger(Themes.class);
				log.error("failed to create folder " + dir, e);
				return noCss();
			}
		}

		// copy the default themes if they are not yet present
		var defaults = new String[]{
			"Dark.css",
			"Default.css",
		};
		for (var name : defaults) {
			var file = new File(dir, name);
			if (file.exists())
				continue;
			try (var stream = Themes.class.getResourceAsStream(name)) {
				if (stream == null)
					continue;
				Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				var log = LoggerFactory.getLogger(Themes.class);
				log.error("failed to copy default theme " + name + " to " + file, e);
			}
		}

		// load all themes from the CSS files in the folder
		var files = dir.listFiles();
		if (files == null)
			return noCss();
		var themes = new ArrayList<Theme>();
		for (var file : files) {
			var theme = Theme.loadFrom(file);
			theme.ifPresent(themes::add);
		}
		return themes.isEmpty() ? noCss() : themes;
	}

	private static List<Theme> noCss() {
		return Collections.singletonList(
			Theme.defaults("no.css", "Default; no CSS"));
	}
}
