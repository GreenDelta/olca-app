package org.openlca.app.util;

import org.openlca.app.rcp.images.Icon;

/**
 * A pop-up for warning messages.
 */
public class WarningPopup extends Popup {

	public WarningPopup(String message) {
		this(null, message);
	}

	public WarningPopup(String title, String message) {
		super(title, message);
		defaultTitle("Warning");
		popupShellImage(Icon.WARNING);
	}

	public static void show(final String message) {
		show(null, message);
	}

	public static void show(final String title, final String message) {
		new WarningPopup(title, message).show();
	}

}
