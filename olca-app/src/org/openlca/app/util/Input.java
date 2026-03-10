package org.openlca.app.util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;

import java.util.function.Function;

public class Input {

	public static String promptString(String title, String message, String defaultValue) {
		return prompt(title, message, defaultValue, (v) -> v, null);
	}

	public static <T> T prompt(String title, String message, String defaultValue, Function<String, T> converter,
			IInputValidator validator) {
		var dialog = new InputDialog(UI.shell(), title, message, defaultValue, validator);
		if (dialog.open() != IDialogConstants.OK_ID)
			return null;
		return converter.apply(dialog.getValue());
	}
}
