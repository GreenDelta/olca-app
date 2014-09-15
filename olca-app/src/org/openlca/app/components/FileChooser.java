package org.openlca.app.components;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;

/**
 * A helper class for selecting a file for an import or export via a file
 * dialog.
 */
public class FileChooser {

	public final static int DIRECTORY_DIALOG = 1;
	public final static int FILE_DIALOG = 0;

	private static String getDialogText(int swtFlag) {
		switch (swtFlag) {
		case SWT.OPEN:
			return Messages.Import;
		case SWT.SAVE:
			return Messages.SelectTheExportFile;
		default:
			return "";
		}
	}

	private static String openDialog(Shell shell, String extension,
			String defaultName, int flag, int swtFlag) {
		switch (flag) {
		case FILE_DIALOG:
			return openFileDialog(shell, extension, defaultName, swtFlag);
		case DIRECTORY_DIALOG:
			return openDirectoryDialog(shell, swtFlag);
		default:
			return null;
		}
	}

	private static String openDirectoryDialog(Shell shell, int swtFlag) {
		DirectoryDialog dialog = new DirectoryDialog(shell, swtFlag);
		dialog.setText(Messages.SelectADirectory);
		return dialog.open();
	}

	private static String openFileDialog(Shell shell, String extension,
			String defaultName, int swtFlag) {
		FileDialog dialog = new FileDialog(shell, swtFlag);
		dialog.setText(getDialogText(swtFlag));
		if (extension != null) {
			dialog.setFilterExtensions(new String[] { extension });
		}
		dialog.setFileName(defaultName);
		return dialog.open();
	}

	private static File selectForPath(String path) {
		File file = new File(path);
		if (!file.exists() || file.isDirectory())
			return file;
		boolean write = MessageDialog.openQuestion(UI.shell(),
				Messages.FileAlreadyExists, Messages.OverwriteFileQuestion);
		if (write)
			return file;
		return null;
	}

	/**
	 * Selects a file/directory for an export. Returns null if the user
	 * cancelled the dialog. Flag indicates if a file or a directory dialog
	 * should be used
	 */
	public static File forExport(int flag) {
		return forExport(null, null, flag);
	}

	/**
	 * Selects a file for an export. Returns null if the user cancelled the
	 * dialog.
	 */
	public static File forExport(String extension, String defaultName) {
		return forExport(extension, defaultName, FILE_DIALOG);
	}

	/**
	 * Selects a file for an export. Returns null if the user cancelled the
	 * dialog. Optional defaultName sets the default file name for save dialogs.
	 * Flag indicates if a file or a directory dialog should be used.
	 */
	private static File forExport(String extension, String defaultName, int flag) {
		Shell shell = UI.shell();
		if (shell == null)
			return null;
		String path = openDialog(shell, extension, defaultName, flag, SWT.SAVE);
		if (path == null)
			return null;
		return selectForPath(path);
	}

	/**
	 * Selects a file/directory for an import. Returns null if the user
	 * cancelled the dialog. flag indicates if a file or directory dialog should
	 * be opened
	 */
	public static File forImport(int flag) {
		return forImport(null, flag);
	}

	/**
	 * Selects a file/directory for an import. Returns null if the user
	 * cancelled the dialog.
	 */
	public static File forImport(String extension) {
		return forImport(extension, FILE_DIALOG);
	}

	/**
	 * Selects a file/directory for an import. Returns null if the user
	 * cancelled the dialog. flag indicates if a file or directory dialog should
	 * be opened
	 */
	private static File forImport(String extension, int flag) {
		Shell shell = UI.shell();
		if (shell == null)
			return null;
		String path = openDialog(shell, extension, null, flag, SWT.OPEN);
		if (path == null)
			return null;
		File file = new File(path);
		if (!file.exists())
			return null;
		return file;
	}
}
