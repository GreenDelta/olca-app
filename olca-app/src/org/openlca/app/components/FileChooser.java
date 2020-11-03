package org.openlca.app.components;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.M;
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
			return M.Import;
		case SWT.SAVE:
			return M.SelectTheExportFile;
		default:
			return "";
		}
	}

	private static String openDialog(Shell shell, String extension,
			String defaultName, String filterPath, int flag, int swtFlag) {
		switch (flag) {
		case FILE_DIALOG:
			return openFileDialog(shell, extension, defaultName, filterPath,
					swtFlag);
		case DIRECTORY_DIALOG:
			return openDirectoryDialog(shell, filterPath, swtFlag);
		default:
			return null;
		}
	}

	private static String openDirectoryDialog(Shell shell, String filterPath,
			int swtFlag) {
		DirectoryDialog dialog = new DirectoryDialog(shell, swtFlag);
		dialog.setText(M.SelectADirectory);
		if (filterPath != null)
			dialog.setFilterPath(filterPath);
		return dialog.open();
	}

	private static String openFileDialog(Shell shell, String extension,
			String defaultName, String filterPath, int swtFlag) {
		FileDialog dialog = new FileDialog(shell, swtFlag);
		dialog.setText(getDialogText(swtFlag));
		String ext = null;
		if (extension != null) {
			ext = extension.trim();
			if (ext.contains("|"))
				ext = ext.substring(0, ext.indexOf("|")).trim();
			dialog.setFilterExtensions(new String[] { ext });
		}
		dialog.setFileName(defaultName);
		if (filterPath != null)
			dialog.setFilterPath(filterPath);
		if (extension != null) {
			if (extension.contains("|")) {
				String label = extension.substring(extension.indexOf("|") + 1);
				label += " (" + ext + ")";
				dialog.setFilterNames(new String[] { label });
			}
		}
		return dialog.open();
	}

	private static File selectForPath(String path) {
		File file = new File(path);
		if (!file.exists() || file.isDirectory())
			return file;
		boolean write = MessageDialog.openQuestion(UI.shell(),
				M.FileAlreadyExists, M.OverwriteFileQuestion);
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
		return forExport(flag, null);
	}

	/**
	 * Selects a file/directory for an export. Returns null if the user
	 * cancelled the dialog. Flag indicates if a file or a directory dialog
	 * should be used
	 */
	public static File forExport(int flag, String filterPath) {
		return forExport(null, null, filterPath, flag);
	}

	/**
	 * Selects a file for an export. Returns null if the user cancelled the
	 * dialog.
	 */
	public static File forExport(String extension, String defaultName) {
		return forExport(extension, defaultName, null);
	}

	/**
	 * Selects a file for an export. Returns null if the user cancelled the
	 * dialog.
	 */
	public static File forExport(String extension, String defaultName,
			String filterPath) {
		return forExport(extension, defaultName, filterPath, FILE_DIALOG);
	}

	/**
	 * Selects a file for an export. Returns null if the user cancelled the
	 * dialog. Optional defaultName sets the default file name for save dialogs.
	 * Flag indicates if a file or a directory dialog should be used.
	 */
	private static File forExport(String extension, String defaultName,
			String filterPath, int flag) {
		Shell shell = UI.shell();
		String path = openDialog(shell, extension, defaultName, filterPath,
				flag, SWT.SAVE);
		if (path == null)
			return null;
		return selectForPath(path);
	}

	public static File openFolder() {
		Shell shell = UI.shell();
		String path = openDialog(
				shell, null, null, null, DIRECTORY_DIALOG, SWT.OPEN);
		if (path == null)
			return null;
		File file = new File(path);
		if (!file.exists() || !file.isDirectory())
			return null;
		return file;
	}


	/**
	 * Selects a file for reading. Returns null if the user cancelled the dialog.
	 */
	public static File open(String extension) {
		Shell shell = UI.shell();
		String path = openDialog(
				shell, extension, null, null, FILE_DIALOG, SWT.OPEN);
		if (path == null)
			return null;
		File file = new File(path);
		if (!file.exists())
			return null;
		return file;
	}
}
