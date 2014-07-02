package org.openlca.app.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.Messages;

/**
 * A helper class for opening message boxes in the UI thread. The methods can be
 * called in other threads the the UI thread.
 */
class MessageBox {

	enum Type {
		ERROR, WARNING, INFO
	}

	private MessageBox() {
	}

	static void show(final String message, Type type) {
		String title = null;
		switch (type) {
		case ERROR:
			title = Messages.Error;
			break;
		case WARNING:
			title = Messages.Warning;
			break;
		case INFO:
			title = Messages.Information;
			break;
		default:
			break;
		}
		show(title, message, type);
	}

	static void show(final String title, final String message, final Type type) {
		new BoxJob(title, message, type).schedule();
	}

	private static class BoxJob extends UIJob {

		private String title;
		private String message;
		private Type type;

		public BoxJob(String title, String message, Type type) {
			super("Open message box");
			this.title = title;
			this.message = message;
			this.type = type;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Display display = getDisplay();
			if (display == null)
				return Status.CANCEL_STATUS;
			Shell shell = display.getActiveShell();
			if(shell == null)
				shell = new Shell(display);
			openBox(shell);
			return Status.OK_STATUS;
		}

		private void openBox(Shell shell) {
			switch (type) {
			case ERROR:
				MessageDialog.openError(shell, title, message);
				break;
			case WARNING:
				MessageDialog.openWarning(shell, title, message);
				break;
			case INFO:
				MessageDialog.openInformation(shell, title, message);
				break;
			default:
				break;
			}
		}
	}

}
