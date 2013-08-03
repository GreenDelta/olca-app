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
 */
public enum Language {

	ENGLISH(org.openlca.app.Messages.English, "en"),

	GERMAN(org.openlca.app.Messages.German, "de");

	private String code;
	private String displayName;

	private Language(final String displayName, final String code) {
		this.displayName = displayName;
		this.code = code;
	}

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

	public String getCode() {
		return code;
	}

	public String getDisplayName() {
		return displayName;
	}

}
