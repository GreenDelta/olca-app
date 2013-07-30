package org.openlca.app;

import org.eclipse.jface.dialogs.MessageDialog;

public class Question {

	private Question() {
	}

	/** Must be called in the UI thread. */
	public static boolean ask(String title, String message) {
		return MessageDialog.openQuestion(UI.shell(), title, message);
	}

}
