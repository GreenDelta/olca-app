package org.openlca.app.components.graphics.themes;

import org.openlca.app.App;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.util.Dirs;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.preferences.Preferences;

import java.util.Set;

public final class Themes {

	public static final String CONTEXT_MODEL = "model";
	public static final String CONTEXT_SANKEY = "sankey";

	public static final String LIGHT = "Light";
	public static final String DARK = "Dark";
	private static final String[] DEFAULTS = new String[]{LIGHT + ".css",
			DARK + ".css", "Nord_Dark.css", "Nord_Light.css", "Poimandres.css"};

	private Themes() {
	}

	public static Theme get(String context) {
		var name = Preferences.get(Preferences.GRAPHICAL_EDITOR_THEME);
		return loadFromWorkspace(context)
				.stream()
				.filter(theme -> name.equalsIgnoreCase(theme.name()))
				.findAny()
				.orElse(noCss().get(0));
	}

	public static List<String> getValidThemeNames() {
		var files = getFiles();
		if (files == null)
			return Collections.emptyList();
		var themes = new ArrayList<String>();
		for (var file : files) {
			if (!file.isFile() || !file.getName().endsWith(".css"))
				continue;
			var model = Theme.loadFrom(file, CONTEXT_MODEL);
			var sankey = Theme.loadFrom(file, CONTEXT_SANKEY);
			if (model.isPresent() && sankey.isPresent()) {
				themes.add(model.get().name());
			}
		}
		return themes;
	}

	public static List<Theme> loadFromWorkspace(String context) {
		var files = getFiles();
		if (files == null)
			return noCss();
		var themes = new ArrayList<Theme>();
		for (var file : files) {
			if (!file.isFile() || !file.getName().endsWith(".css"))
				continue;
			var theme = Theme.loadFrom(file, context);
			theme.ifPresent(themes::add);
		}
		return themes.isEmpty() ? noCss() : themes;
	}

	public static File[] getFiles() {
		var dir = new File(Workspace.root(), "graph-themes");
		unpackDefaultThemes(dir);
		return dir.listFiles();
	}

	private static List<Theme> noCss() {
		return Collections.singletonList(Theme.of("no.css", "Default; no CSS"));
	}

	private static void unpackDefaultThemes(File dir) {
		if (dir == null)
			return;
		try {
			Dirs.createIfAbsent(dir);
			var version = VersionState.readFrom(dir);
			for (var name : DEFAULTS) {
				var file = new File(dir, name);
				if (version.isCurrent() && file.exists())
					continue;
				unpackDefaultTheme(dir, name);
			}
			if (!version.isCurrent()) {
				version.write();
				deleteOldFolders(dir);
			}
		} catch (Exception e) {
			ErrorReporter.on(
					"failed to unpack default graph themes", e);
		}
	}

	private static void unpackDefaultTheme(
			File dir, String name
	) throws IOException {
		var log = LoggerFactory.getLogger(Themes.class);
		log.info("update graph-theme {}", name);
		var stream = Themes.class.getResourceAsStream(name);
		if (stream == null) {
			log.warn("failed to load graph theme {}", name);
			return;
		}
		var file = new File(dir, name);
		try (stream) {
			Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	// TODO: added for version 2.0.3; this should
// be removed later (e.g. version >= 2.1). This
// method deletes the `v1` and `v2` sub-folders
// that were used in older versions
	private static void deleteOldFolders(File dir) {
		try {
			var defaults = Set.of(
					"Dark.css", "Default.css", "Light.css");
			for (var v : List.of("v1", "v2")) {
				var sub = new File(dir, v);
				if (!sub.isDirectory())
					continue;
				boolean canDelete = true;
				var files = sub.list();
				if (files == null) {
					Dirs.delete(sub);
					continue;
				}
				for (var file : files) {
					if (defaults.contains(file))
						continue;
					canDelete = false;
					break;
				}
				if (canDelete) {
					Dirs.delete(sub);
				}
			}
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(Themes.class);
			log.error("failed to delete old theme folders", e);
		}
	}

	private record VersionState(File dir, boolean isCurrent) {

		static VersionState readFrom(File dir) {
			if (dir == null)
				return new VersionState(null, false);
			var file = new File(dir, ".version");
			if (!file.exists())
				return new VersionState(dir, false);
			try {
				var v = Files.readString(file.toPath(), StandardCharsets.UTF_8);
				return new VersionState(dir, v.equals(App.getVersion()));
			} catch (Exception e) {
				var log = LoggerFactory.getLogger(VersionState.class);
				log.error("failed to read version from " + file, e);
				return new VersionState(dir, false);
			}
		}

		void write() {
			if (isCurrent || dir == null)
				return;
			try {
				Dirs.createIfAbsent(dir);
				var file = new File(dir, ".version");
				Files.writeString(
						file.toPath(), App.getVersion(), StandardCharsets.UTF_8);
			} catch (Exception e) {
				var log = LoggerFactory.getLogger(getClass());
				log.error("failed to write version", e);
			}
		}
	}
}
