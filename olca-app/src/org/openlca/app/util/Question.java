package org.openlca.app.util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

public class Question {

	private Question() {
	}

	/** Must be called in the UI thread. */
	public static boolean ask(String title, String message) {
		return MessageDialog.openQuestion(UI.shell(), title, message);
	}

	/** Must be called in the UI thread. */
	public static int askWithCancel(String title, String message) {
		String[] labels = new String[] { IDialogConstants.YES_LABEL,
				IDialogConstants.NO_LABEL,
				IDialogConstants.CANCEL_LABEL };
		MessageDialog dialog = new MessageDialog(
				UI.shell(), title, null, message,
				MessageDialog.QUESTION_WITH_CANCEL, labels, 0);
		int result = dialog.open();
		if (result == 0)
			return IDialogConstants.YES_ID;
		if (result == 1)
			return IDialogConstants.NO_ID;
		if (result == 2)
			return IDialogConstants.CANCEL_ID;
		return IDialogConstants.CANCEL_ID;
	}

	public static int askWithAll(String title, String message) {
		String[] labels = new String[] {
				IDialogConstants.YES_LABEL,
				IDialogConstants.YES_TO_ALL_LABEL,
				IDialogConstants.NO_LABEL,
				IDialogConstants.CANCEL_LABEL };
		MessageDialog dialog = new MessageDialog(
				UI.shell(), title, null, message,
				MessageDialog.QUESTION, labels, 0);
		int result = dialog.open();
		if (result == 0)
			return IDialogConstants.YES_ID;
		if (result == 1)
			return IDialogConstants.YES_TO_ALL_ID;
		if (result == 2)
			return IDialogConstants.NO_ID;
		if (result == 3)
			return IDialogConstants.NO_TO_ALL_ID;
		return IDialogConstants.CANCEL_ID;
	}
	
	public static int ask(String title, String message, String[] answers) {
		return new MessageDialog(UI.shell(), title, null, message, MessageDialog.QUESTION, answers, 0).open();
	}
	
}
