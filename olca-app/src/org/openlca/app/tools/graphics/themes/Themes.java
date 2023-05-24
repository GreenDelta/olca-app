package org.openlca.app.tools.graphics.themes;

import org.openlca.app.rcp.Workspace;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Themes {

	public static String MODEL = "model";
	public static String SANKEY = "sankey";

	private Themes() {
	}

	public static Theme getDefault(String id) {
		var name = org.openlca.app.preferences.Theme.isDark() ? "Dark" : "Light";
		return loadFromWorkspace(id)
			.stream()
			.filter(theme -> name.equalsIgnoreCase(theme.name()))
			.findAny()
			.orElse(noCss().get(0));
	}

	public static Theme get(String file, String id) {
		return loadFromWorkspace(id)
				.stream()
				.filter(theme -> Objects.equals(file, theme.file()))
				.findAny()
				.orElse(noCss().get(0));
	}

	public static List<Theme> loadFromWorkspace(String id) {
		var dir = new File(Workspace.root(), "graph-themes");
		return loadFrom(dir, id);
	}

	public static synchronized List<Theme> loadFrom(File dir, String id) {
		if (dir == null)
			return noCss();

		var v1Dir = new File(dir, "v1");
		var v2Dir = new File(dir, "v2");
		// create the directory structure if it does not exist
		for (var folder : List.of(dir, v1Dir,v2Dir))
			if (!folder.exists()) {
				try {
					Files.createDirectories(folder.toPath());
				} catch (Exception e) {
					var log = LoggerFactory.getLogger(Themes.class);
					log.error("failed to create folder " + folder, e);
					return noCss();
				}
			}

		// create version sub folders if they do not exist and clean openLCA1 themes.
		var rootFiles = dir.listFiles();
		if (rootFiles != null) {
			try {
				Files.createDirectories(v1Dir.toPath());
				Files.createDirectories(v2Dir.toPath());
			} catch (Exception e) {
				var log = LoggerFactory.getLogger(Themes.class);
				log.error("failed to create version sub-folders", e);
			}
			for (var file : rootFiles) {
				var theme = Theme.loadFrom(file, id);
				if (theme.isEmpty())
					continue;
				var subDir = theme.get().version() == 2 ? v2Dir : v1Dir;
				// move the file
				var target = new File(subDir, file.getName());
				try {
					Files.move(file.toPath(), target.toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					var log = LoggerFactory.getLogger(Themes.class);
					log.error("failed to move old themes " + file.getName()
							+ " to " + target, e);
				}
			}
		}


		// copy the default themes if they are not yet present
		var defaults = new String[]{
			"Dark.css",
			"Light.css",
		};
		for (var name : defaults) {
			var file = new File(v2Dir, name);
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
		var files = v2Dir.listFiles();
		if (files == null)
			return noCss();
		var themes = new ArrayList<Theme>();
		for (var file : files) {
			var theme = Theme.loadFrom(file, id);
			theme.ifPresent(themes::add);
		}
		return themes.isEmpty() ? noCss() : themes;
	}

	private static List<Theme> noCss() {
		return Collections.singletonList(
			Theme.defaults("no.css", "Default; no CSS", 0));
	}

}
