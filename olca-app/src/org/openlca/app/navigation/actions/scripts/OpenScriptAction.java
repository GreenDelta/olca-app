package org.openlca.app.navigation.actions.scripts;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.devtools.python.PythonEditor;
import org.openlca.app.devtools.sql.SqlEditor;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.app.rcp.images.Icon;

public class OpenScriptAction extends Action implements INavigationAction {

	private File file;

	public OpenScriptAction() {
		setText(M.Open);
		setImageDescriptor(Icon.FOLDER_OPEN.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection == null || selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof ScriptElement))
			return false;
		var scriptElem = (ScriptElement) first;
		var file = scriptElem.getContent();
		if (file == null || file.isDirectory())
			return false;
		this.file = file;
		return true;
	}

	@Override
	public void run() {
		run(file);
	}

	public static void run(File file) {
		if (file == null
				|| !file.exists()
				|| !file.isFile())
			return;
		var name = file.getName().toLowerCase();
		if (name.endsWith(".sql")) {
			SqlEditor.open(file);
		} else {
			PythonEditor.open(file);
		}
	}
}
