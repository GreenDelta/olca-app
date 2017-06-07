package org.openlca.app.util;

import java.util.function.Function;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;

public class Input {

	public static String promptString(String title, String message, String defaultValue) {
		return prompt(title, message, defaultValue, (v) -> v, null);
	}

	public static Double promptDouble(String title, String message, Double defaultValue) {
		String dValue = defaultValue == null ? "" : Double.toString(defaultValue);
		return prompt(title, message, dValue,
				(v) -> {
					try {
						return Double.parseDouble(v);
					} catch (NumberFormatException e) {
						return null;
					}
				},
				(v) -> v == null ? "" : null);
	}

	private static <T> T prompt(String title, String message, String defaultValue, Function<String, T> converter,
			IInputValidator validator) {
		InputDialog dialog = new InputDialog(UI.shell(), title, message, defaultValue, validator);
		if (dialog.open() != IDialogConstants.OK_ID)
			return null;
		return converter.apply(dialog.getValue());
	}

}
