package org.openlca.app.tools.graphics.themes;

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
import java.util.Objects;

public final class Themes {

	public static String MODEL = "model";
	public static String SANKEY = "sankey";

	private Themes() {
	}

	public static Theme getDefault(String context) {
		var name = org.openlca.app.preferences.Theme.isDark() ? "Dark" : "Light";
		return loadFromWorkspace(context)
				.stream()
				.filter(theme -> name.equalsIgnoreCase(theme.name()))
				.findAny()
				.orElse(noCss().get(0));
	}

	public static Theme get(String file, String context) {
		return loadFromWorkspace(context)
				.stream()
				.filter(theme -> Objects.equals(file, theme.file()))
				.findAny()
				.orElse(noCss().get(0));
	}

	public static List<Theme> loadFromWorkspace(String context) {
		var dir = new File(Workspace.root(), "graph-themes");
		unpackDefaultThemes(dir);
		var files = dir.listFiles();
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

	private static List<Theme> noCss() {
		return Collections.singletonList(Theme.of("no.css", "Default; no CSS"));
	}

	private static void unpackDefaultThemes(File dir) {
		if (dir == null)
			return;
		try {
			Dirs.createIfAbsent(dir);
			var version = VersionState.readFrom(dir);
			var defaults = new String[]{"Dark.css", "Light.css"};
			for (var name : defaults) {
				var file = new File(dir, name);
				if (version.isCurrent() && file.exists())
					continue;
				unpackDefaultTheme(dir, name);
			}
			if (!version.isCurrent()) {
				version.write();
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
