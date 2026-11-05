package org.openlca.app.preferences;

import java.util.Locale;
import java.util.Objects;

import org.openlca.app.M;

/// Enumeration of supported languages.
public enum Language {

	ARABIC(M.Arabic, "ar"),

	CHINESESIMPLIFIED(M.ChineseSimplified, "zh_cn"),

	ENGLISH(M.English, "en"),

	FRENCH(M.French, "fr"),

	GERMAN(M.German, "de"),

	INDONESIAN(M.Indonesian, "id"),

	ITALIAN(M.Italian, "it"),

	KOREAN(M.Korean, "ko"),

	SPANISH(M.Spanish, "es");

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

	/// Returns the language of the user interface. This is the language from
	/// the [AppConfig], if configured, or otherwise the language of the
	/// default locale (which is set from the `-nl` flag of the `openLCA.ini`
	/// file). If no supported language matches, English is returned.
	public static Language getApplicationLanguage() {
		var code = AppConfig.read().languageCode();
		if (code != null) {
			var lang = getForCode(code.toLowerCase(Locale.ROOT));
			if (lang != null)
				return lang;
		}
		var locale = Locale.getDefault();
		var lang = getForCode(locale.toString().toLowerCase(Locale.ROOT));
		if (lang != null)
			return lang;
		lang = getForCode(locale.getLanguage());
		return lang != null ? lang : ENGLISH;
	}

}
