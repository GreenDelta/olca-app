package org.openlca.app.navigation.actions.scripts;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;

public class RenameScriptAction extends Action implements INavigationAction {

	// invalid Windows file name characters: \ / : * ? " < > |
	private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

	private File file;

	public RenameScriptAction() {
		setText(M.Rename);
		setImageDescriptor(Icon.CHANGE.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		if (!(selection.getFirst() instanceof ScriptElement e))
			return false;
		var file = e.getContent();
		if (file == null || file.isDirectory() || !file.exists())
			return false;
		this.file = file;
		return true;
	}

	@Override
	public void run() {
		if (file == null)
			return;

		var currentName = file.getName();
		var extension = getExtension(currentName);
		var baseName = getBaseName(currentName);

		var dialog = new InputDialog(
				UI.shell(),
				M.Rename,
				M.PleaseEnterANewName,
				baseName,
				newName -> validateName(newName, extension));

		if (dialog.open() != Window.OK)
			return;

		var newBaseName = dialog.getValue().trim();
		if (newBaseName.isEmpty())
			return;

		// re-add extension if the user removed it
		var newName = newBaseName.endsWith(extension)
				? newBaseName
				: newBaseName + extension;

		var newFile = new File(file.getParentFile(), newName);
		if (newFile.exists()) {
			MsgBox.error(M.Rename, M.FileAlreadyExists);
			return;
		}

		if (!file.renameTo(newFile)) {
			MsgBox.error("Failed to rename file", "Renaming the script failed");
			return;
		}

		Navigator.refresh();
	}

	private String validateName(String name, String extension) {
		if (name == null || name.trim().isEmpty())
			return M.NameCannotBeEmpty;

		var trimmed = name.trim();

		// check for invalid characters
		if (INVALID_CHARS.matcher(trimmed).find())
			return "The name contains invalid characters for a file name.";

		// check for reserved Windows names
		var baseName = trimmed.endsWith(extension)
				? trimmed.substring(0, trimmed.length() - extension.length())
				: trimmed;
		if (isReservedWindowsName(baseName))
			return "This file name is not allowed.";

		// check if name ends with a dot or space (invalid on Windows)
		if (trimmed.endsWith("."))
			return "The name cannot end with a dot.";

		return null;
	}

	private boolean isReservedWindowsName(String name) {
		var upper = name.toUpperCase();
		// Reserved Windows file names
		return upper.equals("CON") || upper.equals("PRN")
				|| upper.equals("AUX") || upper.equals("NUL")
				|| upper.matches("COM[1-9]") || upper.matches("LPT[1-9]");
	}

	private String getExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex > 0) {
			return fileName.substring(dotIndex);
		}
		return "";
	}

	private String getBaseName(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex > 0) {
			return fileName.substring(0, dotIndex);
		}
		return fileName;
	}
}
