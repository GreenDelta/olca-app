/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.preferencepages;

/**
 * Enumeration of supported languages
 * 
 * @author Sebastian Greve
 * 
 */
public enum Language {

	/**
	 * Enum value for english
	 */
	ENGLISH(org.openlca.app.Messages.English, "en"),

	/**
	 * Enum value for german
	 */
	GERMAN(org.openlca.app.Messages.German, "de");

	/**
	 * The language code
	 */
	private String code;

	/**
	 * The display name of the language
	 */
	private String displayName;

	/**
	 * Creates a new instance
	 * 
	 * @param displayName
	 *            The display name of the language
	 * @param code
	 *            The language code
	 */
	private Language(final String displayName, final String code) {
		this.displayName = displayName;
		this.code = code;
	}

	/**
	 * Returns the language enum value with the given code
	 * 
	 * @param code
	 *            The code of the requested language
	 * @return The language enum value with the given code
	 */
	public static Language getLanguage(final String code) {
		Language language = null;
		int i = 0;
		while (language == null && i < Language.values().length) {
			if (Language.values()[i].getCode().equals(code.substring(0, 2))) {
				language = Language.values()[i];
			} else {
				i++;
			}
		}
		return language;
	}

	/**
	 * Getter of the code
	 * 
	 * @return The language code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Getter of the display name
	 * 
	 * @return The display name of the language
	 */
	public String getDisplayName() {
		return displayName;
	}

}
