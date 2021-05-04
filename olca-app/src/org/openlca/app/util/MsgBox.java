package org.openlca.app.util;

import java.util.function.Consumer;

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
		ERROR, WARNING, INFO, QUESTION;
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

	public static void question(String title, String text, Consumer<Boolean> callback) {
		new BoxJob(title, text, Type.QUESTION, callback).schedule();
	}

	private static class BoxJob extends UIJob {

		private String title;
		private String message;
		private Type type;
		private Consumer<Boolean> callback;

		public BoxJob(String title, String message, Type type) {
			this(title, message, type, null);
		}

		public BoxJob(String title, String message, Type type, Consumer<Boolean> callback) {
			super("Open message box");
			this.title = title == null ? "?" : title;
			this.message = message == null ? "?" : message;
			this.type = type;
			this.callback = callback;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Display display = getDisplay();
			if (display == null)
				return Status.CANCEL_STATUS;
			Shell shell = display.getActiveShell();
			if (shell == null)
				shell = new Shell(display);
			boolean result = openBox(shell);
			if (callback != null) {
				callback.accept(result);
			}
			return Status.OK_STATUS;
		}

		private boolean openBox(Shell shell) {
			switch (type) {
			case ERROR:
				MessageDialog.openError(shell, title, message);
				return false;
			case WARNING:
				MessageDialog.openWarning(shell, title, message);
				return false;
			case INFO:
				MessageDialog.openInformation(shell, title, message);
				return false;
			case QUESTION:
				return MessageDialog.openQuestion(shell, title, message);
			default:
				return false;
			}
		}
	}

}
