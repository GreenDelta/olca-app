package org.openlca.app.util;

import org.openlca.app.rcp.ImageType;

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
		popupShellImage(ImageType.WARNING_ICON);
	}

	public static void show(final String message) {
		show(null, message);
	}

	public static void show(final String title, final String message) {
		new WarningPopup(title, message).show();
	}

}
