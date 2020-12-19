package org.openlca.app.navigation.actions.libraries;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.util.Dirs;

public class DeleteLibraryAction extends Action implements INavigationAction {

	private LibraryElement element;

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (first instanceof LibraryElement) {
			this.element = (LibraryElement) first;
			return true;
		}
		return false;
	}

	@Override
	public String getText() {
		return "Remove library";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DELETE.descriptor();
	}

	@Override
	public void run() {
		if (element == null)
			return;
		var lib = element.getContent();
		if (lib == null)
			return;

		// check if this is a mounted library
		var db = element.getDatabase();
		if (db.isPresent()) {
			// TODO: unmount a library from a database
			MsgBox.info("Not yet supported",
					"Removing a library from a database is not yet supported.");
			return;
		}

		// ask and delete the library
		boolean b = Question.ask("Remove library?",
				"There is currently no check whether this library" +
						" is used in other databases. Removing this library " +
						"can make these databases unusable. Do you want to " +
						"proceed?");
		if (!b)
			return;
		var dir = Workspace.getLibraryDir()
				.getFolder(lib.getInfo());
		Dirs.delete(dir);
		Navigator.refresh();
	}
}
