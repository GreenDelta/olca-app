package org.openlca.app.preferences;

import java.util.Objects;

/**
 * Enumeration of supported languages.
 */
public enum Language {

	ARABIC("Arabic", "ar"),

	BULGARIAN("Bulgarian", "bg"),

	CATALAN("Catalan", "ca"),

	CHINESE("Chinese", "zh"),

	ENGLISH("English", "en"),

	FRENCH("French", "fr"),

	GERMAN("German", "de"),

	HUNGARIAN("Hungarian", "hu"),
	
	ITALIAN("Italian", "it"),

	PORTUGUESE("Portuguese", "pt"),

	SPANISH("Spanish", "es"),

	TURKISH("Turkish", "tr");

	private String code;
	private String displayName;

	private Language(String displayName, String code) {
		this.displayName = displayName;
		this.code = code;
	}

	public static Language getForCode(String code) {
		for (Language language : values()) {
			if (Objects.equals(code, language.code))
				return language;
		}
		return null;
	}

	public static Language getForDisplayName(String name) {
		for (Language language : values()) {
			if (Objects.equals(name, language.displayName))
				return language;
		}
		return null;
	}

	public String getCode() {
		return code;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static Language getApplicationLanguage() {
		return ConfigIniFile.read().getLanguage();
	}

}
