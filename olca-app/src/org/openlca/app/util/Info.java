package org.openlca.app.util;

import org.openlca.app.util.MessageBox.Type;

public class Info {

	private Info() {
	}

	public static void showBox(String message) {
		MessageBox.show(message, Type.INFO);
	}

	public static void showBox(String title, String message) {
		MessageBox.show(title, message, Type.INFO);
	}
}
