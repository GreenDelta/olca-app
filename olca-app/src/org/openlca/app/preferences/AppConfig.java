package org.openlca.app.preferences;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import org.eclipse.osgi.util.NLS;
import org.openlca.util.OS;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/// User specific application settings that need to be applied before the
/// workbench starts, like the user interface language. They are stored in an
/// `app.json` file in the user's configuration folder:
///
/// - Linux: `$XDG_CONFIG_HOME/openLCA/app.json` or `~/.config/openLCA/app.json`
/// - Windows: `%APPDATA%\openLCA\app.json`
/// - macOS: `~/Library/Application Support/openLCA/app.json`
///
/// Settings that are not present in that file fall back to the values of the
/// `openLCA.ini` file, e.g. the `-nl` flag that is set by the installer.
///
/// Note that this class must not use the [Language] enumeration or the `M`
/// messages because it is used in the bundle activator before the locale is
/// set. Initializing the messages there would fix them to the wrong language.
public class AppConfig {

	private static final String LANGUAGE = "language";
	private static final String USE_EDGE_BROWSER = "useEdgeBrowser";
	private static final String BROWSER_TYPE_PROP =
			"org.eclipse.swt.browser.DefaultType";

	/// The raw JSON content; unknown fields are kept when writing the file.
	private final JsonObject json;

	private AppConfig(JsonObject json) {
		this.json = json;
	}

	public static AppConfig read() {
		var file = file();
		if (!file.exists())
			return new AppConfig(new JsonObject());
		try {
			var text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			var elem = JsonParser.parseString(text);
			return elem.isJsonObject()
					? new AppConfig(elem.getAsJsonObject())
					: new AppConfig(new JsonObject());
		} catch (Exception e) {
			LoggerFactory.getLogger(AppConfig.class)
					.error("failed to read app config: {}", file, e);
			return new AppConfig(new JsonObject());
		}
	}

	/// Writes the configuration to the `app.json` file. Returns `false` if this
	/// failed.
	public boolean write() {
		var file = file();
		try {
			Files.createDirectories(file.getParentFile().toPath());
			var text = new GsonBuilder()
					.setPrettyPrinting()
					.create()
					.toJson(json);
			Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
			return true;
		} catch (Exception e) {
			LoggerFactory.getLogger(AppConfig.class)
					.error("failed to write app config: {}", file, e);
			return false;
		}
	}

	public static File file() {
		var home = System.getProperty("user.home");
		var dir = switch (OS.get()) {
			case WINDOWS -> {
				var appData = System.getenv("APPDATA");
				yield appData != null && !appData.isBlank()
						? new File(appData)
						: new File(home, "AppData/Roaming");
			}
			case MAC -> new File(home, "Library/Application Support");
			default -> {
				var xdg = System.getenv("XDG_CONFIG_HOME");
				yield xdg != null && !xdg.isBlank()
						? new File(xdg)
						: new File(home, ".config");
			}
		};
		return new File(new File(dir, "openLCA"), "app.json");
	}

	/// Applies the settings that need to be set before the workbench and the
	/// message classes are initialized. This is called from the bundle
	/// activator.
	public void applyOnStartup() {
		var lang = languageCode();
		if (lang != null) {
			var locale = localeOf(lang);
			if (!locale.equals(Locale.getDefault())) {
				Locale.setDefault(locale);
				resetNlsSuffixes();
			}
		}
		var useEdge = useEdgeBrowser();
		// same semantics as the old ini-flag: if not set, the SWT default is used
		if (useEdge != null && OS.get() == OS.WINDOWS) {
			if (useEdge) {
				System.setProperty(BROWSER_TYPE_PROP, "edge");
			} else {
				System.clearProperty(BROWSER_TYPE_PROP);
			}
		}
	}

	/// Returns the language code that is stored in the configuration, e.g.
	/// `de` or `zh_cn`, or `null` if no language is configured.
	public String languageCode() {
		var lang = stringOf(LANGUAGE);
		return lang != null && !lang.isBlank()
				? lang.strip()
				: null;
	}

	public void languageCode(String code) {
		if (code == null) {
			json.remove(LANGUAGE);
		} else {
			json.addProperty(LANGUAGE, code);
		}
	}

	/// Returns the configured browser setting, or `null` if not configured.
	/// In the latter case, the setting of the `openLCA.ini` file is used.
	public Boolean useEdgeBrowser() {
		var elem = json.get(USE_EDGE_BROWSER);
		if (elem == null || !elem.isJsonPrimitive())
			return null;
		var prim = elem.getAsJsonPrimitive();
		return prim.isBoolean() ? prim.getAsBoolean() : null;
	}

	public void useEdgeBrowser(boolean b) {
		json.addProperty(USE_EDGE_BROWSER, b);
	}

	/// Returns `true` if the Edge browser is currently used by SWT.
	public static boolean isEdgeBrowserActive() {
		return "edge".equalsIgnoreCase(System.getProperty(BROWSER_TYPE_PROP));
	}

	/// The Eclipse `NLS` class computes the file suffixes of the message
	/// bundles (e.g. `_de`) from the default locale only once, when the first
	/// message class is initialized, and caches them in the private static
	/// field `nlSuffixes`. This already happens during the framework start,
	/// with the locale of the `-nl` flag of the `openLCA.ini` file. We reset
	/// this cache, so that message classes that are initialized from now on,
	/// like `M` or the workbench messages, are loaded for the new locale.
	/// Message classes that were initialized before keep their language.
	private static void resetNlsSuffixes() {
		try {
			var field = NLS.class
					.getDeclaredField("nlSuffixes");
			field.setAccessible(true);
			field.set(null, null);
		} catch (Exception e) {
			LoggerFactory.getLogger(AppConfig.class).warn(
					"failed to reset NLS suffixes; messages may not be translated",
					e);
		}
	}

	/// Creates the locale for a language code like `de` or `zh_cn` in the same
	/// way as Equinox does it for the `-nl` flag.
	static Locale localeOf(String code) {
		var parts = code.split("_");
		return switch (parts.length) {
			case 1 -> Locale.of(parts[0]);
			case 2 -> Locale.of(parts[0], parts[1]);
			default -> Locale.of(parts[0], parts[1], parts[2]);
		};
	}

	private String stringOf(String key) {
		var elem = json.get(key);
		if (elem == null || !elem.isJsonPrimitive())
			return null;
		var prim = elem.getAsJsonPrimitive();
		return prim.isString() ? prim.getAsString() : null;
	}
}
