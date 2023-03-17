package org.openlca.app.components;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

/**
 * A helper class for selecting a file for an import or export via a file
 * dialog.
 */
public class FileChooser {

	/**
	 * Opens a directory dialog for selecting a folder. Returns null when the user
	 * cancelled the action.
	 */
	public static File selectFolder() {
		var dialog = new DirectoryDialog(UI.shell());
		dialog.setText(M.SelectADirectory);
		var path = dialog.open();
		if (path == null)
			return null;
		var folder = new File(path);
		return folder.isDirectory()
				? folder
				: null;
	}

	/**
	 * Opens a file dialog to save a file for saving. We ask the user if the file
	 * should be overwritten if it already exists. The file extension is
	 * determined from the given default name.
	 *
	 * @param title       the text that we display in the title bar of the dialog
	 * @param defaultName the default name of the file
	 * @return the file which the user selected or {@code null} if the user
	 * cancelled the selection of a file.
	 */
	public static File forSavingFile(String title, String defaultName) {
		var dialog = new FileDialog(UI.shell(), SWT.SAVE);
		dialog.setText(title == null ? M.Save : title);

		if (defaultName != null) {
			dialog.setFileName(defaultName);
			var parts = defaultName.split("\\.");
			if (parts.length > 1) {
				var ext = parts[parts.length - 1];
				dialog.setFilterExtensions(new String[]{"*." + ext});
			}
		}

		var path = dialog.open();
		if (path == null)
			return null;
		var file = new File(path);
		if (file.exists()) {
			boolean write = MessageDialog.openQuestion(
					UI.shell(), M.FileAlreadyExists, M.OverwriteFileQuestion);
			if (!write)
				return null;
		}
		return file;
	}

	/**
	 * Selects a file for reading. Returns {@code null} if the user cancelled the
	 * dialog.
	 */
	public static File open(String extension) {
		return openFile()
				.withExtensions(extension)
				.select()
				.orElse(null);
	}

	public static OpenBuilder openFile() {
		return new OpenBuilder();
	}

	public static class OpenBuilder {

		private String title;
		private String[] extensions;

		public OpenBuilder withTitle(String title) {
			this.title = title;
			return this;
		}

		public OpenBuilder withExtensions(String... extensions) {
			this.extensions = extensions;
			return this;
		}

		public Optional<File> select() {
			var dialog = new FileDialog(UI.shell(), SWT.OPEN);
			dialog.setText(this.title == null ? M.Open : this.title);
			applyExtensionFilter(dialog);
			var path = dialog.open();
			if (Strings.nullOrEmpty(path))
				return Optional.empty();
			var file = new File(path);
			return file.exists()
					? Optional.of(file)
					: Optional.empty();
		}

		public List<File> selectMultiple() {
			var dialog = new FileDialog(UI.shell(), SWT.OPEN | SWT.MULTI);
			dialog.setText(this.title == null ? M.Open : this.title);
			applyExtensionFilter(dialog);
			var firstPath = dialog.open();
			if (firstPath == null)
				return List.of();
			var first = new File(firstPath);
			if (!first.exists())
				return List.of();
			var dir = first.isDirectory()
					? first
					: first.getParentFile();
			var fs = dialog.getFileNames();
			if (fs == null || fs.length == 0)
				return first.isFile() ? List.of(first) : List.of();
			return Arrays.stream(fs)
					.map(fi -> new File(dir, fi))
					.filter(File::exists)
					.toList();
		}

		private void applyExtensionFilter(FileDialog d) {
			if (extensions == null || extensions.length == 0)
				return;
			var extFilter = "";
			for (var ext : extensions) {
				if (Strings.nullOrEmpty(ext))
					continue;
				var e = ext.startsWith("*.")
						? ext
						: "*." + ext;
				extFilter = extFilter.isEmpty()
						? e
						: extFilter + ";" + e;
			}
			if (!extFilter.isEmpty()) {
				d.setFilterExtensions(new String[]{extFilter});
			}
		}
	}
}
