package org.openlca.app.navigation.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.ScriptElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.util.Dirs;

public class DeleteScriptAction extends Action implements INavigationAction {

	private final List<ScriptElement> elements = new ArrayList<>();

	@Override
	public boolean accept(INavigationElement<?> elem) {
		elements.clear();
		if (!(elem instanceof ScriptElement))
			return false;
		elements.add((ScriptElement) elem);
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elems) {
		elements.clear();
		for (var elem : elems) {
			if (elem instanceof ScriptElement) {
				this.elements.add((ScriptElement) elem);
			}
		}
		return !elements.isEmpty();
	}

	@Override
	public String getText() {
		return M.Delete;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DELETE.descriptor();
	}

	@Override
	public void run() {
		for (var elem : elements) {
			var file = elem.getContent();
			if (!file.exists())
				continue;

			// delete script folders
			if (file.isDirectory()) {
				var b = Question.ask("Delete all files in folder",
						"Do you want to delete all files in "
								+ file.getName() + "?");
				if (!b)
					break;
				Dirs.delete(file.getPath());
				continue;
			}

			// delete script files
			var b = Question.ask("Delete file",
					"Do you want to delete the file "
							+ file.getName() + "?");
			if (!b)
				break;
			if (!file.delete()) {
				MsgBox.info("Failed to delete file: " + file.getName());
			}
		}

		Navigator.refresh();
	}
}
