package org.openlca.app.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.M;

/**
 * A helper class for opening message boxes in the UI thread. The methods can be
 * called from other threads than the UI thread.
 */
public class MsgBox {

	enum Type {
		ERROR, WARNING, INFO
	}

	private MsgBox() {
	}

	public static void info(String text) {
		info(M.Information, text);
	}

	public static void info(String title, String text) {
		new BoxJob(title, text, Type.INFO).schedule();
	}

	public static void warning(String text) {
		warning(M.Warning, text);
	}

	public static void warning(String title, String text) {
		new BoxJob(title, text, Type.WARNING).schedule();
	}

	public static void error(String text) {
		error(M.Error, text);
	}

	public static void error(String title, String text) {
		new BoxJob(title, text, Type.ERROR).schedule();
	}

	private static class BoxJob extends UIJob {

		private final String title;
		private final String message;
		private final Type type;

		public BoxJob(String title, String message, Type type) {
			super("Open message box");
			this.title = title == null ? "?" : title;
			this.message = message == null ? "?" : message;
			this.type = type;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			var shell = UI.shell();
			if (shell == null) {
				Display display = getDisplay();
				if (display == null)
					return Status.CANCEL_STATUS;
				shell = display.getActiveShell();
				if (shell == null)
					shell = new Shell(display);
			}
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
