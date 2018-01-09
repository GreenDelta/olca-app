package org.openlca.app.cloud;

import org.openlca.app.M;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.util.UI;

public class TokenDialog {

	private Integer token;

	private TokenDialog() {
	}

	private Integer open() {
		Display.getDefault().syncExec(() -> {
			InputDialog dialog = new InputDialog(UI.shell(), M.EnterYourAuthenticatorToken,
					M.PleaseEnterYourAuthenticatorTokenToProceed, null, TokenDialog::checkValid);
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			token = Integer.parseInt(dialog.getValue());
		});
		return token;
	}

	public static Integer prompt() {
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
