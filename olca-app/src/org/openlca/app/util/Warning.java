package org.openlca.app.util;

import org.openlca.app.util.MessageBox.Type;

public class Warning {

	private Warning() {
	}

	public static void showPopup(String message) {
		new WarningPopup(message).show();
	}

	public static void showPopup(String title, String message) {
		new WarningPopup(title, message).show();
	}

	public static void showBox(String message) {
		MessageBox.show(message, Type.WARNING);
	}

	public static void showBox(String title, String message) {
		MessageBox.show(title, message, Type.WARNING);
	}

}
