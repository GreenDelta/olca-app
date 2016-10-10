package org.openlca.app.cloud;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.util.UI;

public class TokenDialog {

	private int token;

	private TokenDialog() {
	}

	private int open() {
		try {
			Display.getDefault().syncExec(() -> {
				InputDialog dialog = new InputDialog(UI.shell(), "#Enter your authenticator token",
						"#Please enter your authenticator token to proceed", null, TokenDialog::checkValid);
				if (dialog.open() != IDialogConstants.OK_ID)
					return;
				token = Integer.parseInt(dialog.getValue());
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return token;
	}

	public static int prompt() {
		return new TokenDialog().open();
	}

	private static String checkValid(String input) {
		if (input == null)
			return "";
		if (input.length() != 6)
			return "";
		try {
			Integer.parseInt(input);
			return null;
		} catch (Exception e) {
			return "";
		}
	}

}
