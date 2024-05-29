package org.openlca.app.preferences;

import java.util.Objects;

import org.openlca.app.M;

/**
 * Enumeration of supported languages.
 */
public enum Language {

	ARABIC(M.Arabic, "ar"),

	BULGARIAN(M.Bulgarian, "bg"),

	CATALAN(M.Catalan, "ca"),

	CHINESESIMPLIFIED(M.ChineseSimplified, "zh_cn"),

	CHINESETRADITIONAL(M.ChineseTraditional, "zh_tw"),

	ENGLISH(M.English, "en"),

	FRENCH(M.French, "fr"),

	GERMAN(M.German, "de"),

	HUNGARIAN(M.Hungarian, "hu"),

	ITALIAN(M.Italian, "it"),

	PORTUGUESE(M.Portuguese, "pt"),

	SPANISH(M.Spanish, "es"),

	TURKISH(M.Turkish, "tr");

	private final String code;
	private final String displayName;

	Language(String displayName, String code) {
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
		return ConfigIniFile.read().language();
	}

}
