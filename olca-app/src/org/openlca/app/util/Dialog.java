package org.openlca.app.util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.Messages;

/**
 * Some functions for showing pop-up dialogs.
 */
public final class Dialog {

	private Dialog() {
	}

	public static void showError(Shell shell, String error) {
		MessageDialog.openError(shell, Messages.ERROR, error);
	}

	public static void showWarning(Shell shell, String warning) {
		MessageDialog.openWarning(shell, Messages.WARNING, warning);
	}

	public static void showInfo(Shell shell, String information) {
		MessageDialog.openInformation(shell, Messages.INFO, information);
	}

}
