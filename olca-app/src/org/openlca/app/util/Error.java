package org.openlca.app.util;

import org.openlca.app.util.MessageBox.Type;

public class Error {

	private Error() {
	}

	public static void showBox(String message) {
		MessageBox.show(message, Type.ERROR);
	}

	public static void showBox(final String title, final String message) {
		MessageBox.show(title, message, Type.ERROR);
	}

}
