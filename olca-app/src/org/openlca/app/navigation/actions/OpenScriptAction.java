package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.devtools.python.PythonEditor;
import org.openlca.app.devtools.sql.SqlEditor;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ScriptElement;
import org.openlca.app.rcp.images.Icon;

public class OpenScriptAction extends Action implements INavigationAction {

	private File file;

	public OpenScriptAction() {
		setText(M.Open);
		setImageDescriptor(Icon.FOLDER_OPEN.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> elem) {
		if (!(elem instanceof ScriptElement))
			return false;
		var scriptElem = (ScriptElement) elem;
		var file = scriptElem.getContent();
		if (file == null || file.isDirectory())
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
