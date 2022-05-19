package org.openlca.app.util;

import java.util.function.Function;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;

public class Input {

	public static String promptPassword(String title, String message, String defaultValue) {
		var dialog = new InputDialog(UI.shell(), title, message, defaultValue, null) {
			@Override
			protected int getInputTextStyle() {
				return SWT.SINGLE | SWT.BORDER | SWT.PASSWORD;
			}
		};
		if (dialog.open() != IDialogConstants.OK_ID)
			return null;
		return dialog.getValue();
	}
	
	public static String promptString(String title, String message, String defaultValue) {
		return prompt(title, message, defaultValue, (v) -> v, null);
	}

	public static Double promptDouble(String title, String message, Double defaultValue) {
		var dValue = defaultValue == null ? "" : Double.toString(defaultValue);
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

	public static <T> T prompt(String title, String message, String defaultValue, Function<String, T> converter,
			IInputValidator validator) {
		var dialog = new InputDialog(UI.shell(), title, message, defaultValue, validator);
		if (dialog.open() != IDialogConstants.OK_ID)
			return null;
		return converter.apply(dialog.getValue());
	}

}
