package org.openlca.app.preferences;

import org.eclipse.swt.widgets.Display;

import java.util.Objects;

import static org.openlca.util.OS.WINDOWS;

/**
 * Enumeration of supported themes.
 * TODO: Theme cannot be modified within the config. This class is thus not
 *  necessary. It is kept anyway as we might implement it later on.
 *  (see org.openlca.app.preferences.ConfigPage)
 */
public enum Theme {

	DEFAULT("Default", "default"),
	DARK("Dark", "dark");

	private final String name;
	private final String code;

	Theme(String name, String code) {
		this.name = name;
		this.code = code;
	}

	public static Theme getForName(String name) {
		for (Theme theme : values()) {
			if (Objects.equals(name, theme.name))
				return theme;
		}
		return null;
	}

	public static Theme getForCode(String code) {
		for (Theme theme : values()) {
			if (Objects.equals(code, theme.code))
				return theme;
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public static Theme getApplicationTheme() {
		return ConfigIniFile.read().theme();
	}

	public static boolean isDark() {
		return Display.isSystemDarkTheme();
	}

}
