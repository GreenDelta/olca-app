package org.openlca.app.util;

import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MessageBox.Type;

public class Info {

	private Info() {
	}

	public static void popup(String message) {
		popup(null, message);
	}

	public static void popup(String title, String message) {
		Popup popup = new Popup(title, message);
		popup.defaultTitle(M.Notification);
		popup.popupShellImage(Icon.INFO);
		popup.show();
	}

	public static void showBox(String message) {
		MessageBox.show(message, Type.INFO);
	}

	public static void showBox(String title, String message) {
		MessageBox.show(title, message, Type.INFO);
	}
}
