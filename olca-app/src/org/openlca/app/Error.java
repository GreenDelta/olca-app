package org.openlca.app;

import org.openlca.app.MessageBox.Type;

public class Error {

	private Error() {
	}

	public static void showPopup(String message) {
		ErrorPopup.show(message);
	}

	public static void showPopup(String title, String message) {
		ErrorPopup.show(title, message);
	}

	public static void showBox(String message) {
		MessageBox.show(message, Type.ERROR);
	}

	public static void showBox(final String title, final String message) {
		MessageBox.show(title, message, Type.ERROR);
	}

}
