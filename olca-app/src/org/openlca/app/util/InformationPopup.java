package org.openlca.app.util;

import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;

/**
 * A pop-up for information messages.
 */
public class InformationPopup extends Popup {

	public InformationPopup(String message) {
		this(null, message);
	}

	public InformationPopup(String title, String message) {
		super(title, message);
		defaultTitle(Messages.Notification);
		popupShellImage(ImageType.INFO_ICON);
	}

	public static void show(final String message) {
		show(null, message);
	}

	public static void show(final String title, final String message) {
		new InformationPopup(title, message).show();
	}

}
