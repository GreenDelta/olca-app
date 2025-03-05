package org.openlca.app.navigation.actions.libraries;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.DataPackageElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.editors.libraries.LibraryEditor;
import org.openlca.core.library.Library;

public class OpenLibraryAction extends Action implements INavigationAction {

	private Library library;

	public OpenLibraryAction() {
		setText(M.Open);
		setImageDescriptor(Icon.FOLDER_OPEN.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection == null || selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DataPackageElement elem) || !elem.getContent().isLibrary())
			return false;
		library = Workspace.getLibraryDir().getLibrary(
				elem.getContent().name()).orElse(null);
		return library != null;
	}

	@Override
	public void run() {
		if (library == null)
			return;
		LibraryEditor.open(library);
	}
}
