package org.openlca.app;

import org.openlca.app.MessageBox.Type;

public class Info {

	private Info() {
	}

	// TODO show popup
	/**
	 * Added by Georg.
	 */
	public static void showPopup(String message) {
		new InformationPopup(message).show();
	}

	/**
	 * Added by Georg.
	 */
	public static void showPopup(String title, String message) {
		new InformationPopup(title, message).show();
	}

	public static void showBox(String message) {
		MessageBox.show(message, Type.INFO);
	}

	public static void showBox(final String title, final String message) {
		MessageBox.show(title, message, Type.INFO);
	}
}
