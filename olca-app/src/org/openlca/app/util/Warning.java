package org.openlca.app.util;

import org.openlca.app.util.MessageBox.Type;

public class Warning {

	private Warning() {
	}

	public static void showBox(String message) {
		MessageBox.show(message, Type.WARNING);
	}

	public static void showBox(String title, String message) {
		MessageBox.show(title, message, Type.WARNING);
	}

}
