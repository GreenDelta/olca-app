package org.openlca.app.navigation.actions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ScriptElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;

public class ExportScriptAction extends Action implements INavigationAction {

	private File file;

	public ExportScriptAction() {
		setText(M.Export);
		setImageDescriptor(Icon.EXPORT.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> elem) {
		if (!(elem instanceof ScriptElement))
			return false;
		var scriptElem = (ScriptElement) elem;
		var file = scriptElem.getContent();
		if (file == null
				|| file.isDirectory()
				|| !file.exists())
			return false;
		this.file = file;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elems) {
		return false;
	}

	@Override
	public void run() {
		if (file == null)
			return;
		var parts = file.getName().split("\\.");
		var ext = parts.length > 1
				? parts[parts.length - 1]
				: "";
		var target = FileChooser.forExport(ext, file.getName());
		if (target == null)
			return;
		try {
			Files.copy(
					file.toPath(),
					target.toPath(),
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES);
		} catch (Exception e) {
			MsgBox.error("Failed to export script",
					"The export failed with the following error: " + e.getMessage());
		}
	}
}
