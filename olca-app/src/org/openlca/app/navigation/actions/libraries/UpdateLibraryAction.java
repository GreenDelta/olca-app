package org.openlca.app.navigation.actions.libraries;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.library.Unmounter;

public class UpdateLibraryAction extends Action implements INavigationAction {

	private LibraryElement element;

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (first instanceof LibraryElement) {
			this.element = (LibraryElement) first;
			if (this.element.getContent() == null)
				return false;
			return this.element.getDatabase().isPresent();
		}
		return false;
	}

	@Override
	public String getText() {
		return "Update library (experimental)";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.UPDATE.descriptor();
	}

	@Override
	public void run() {
		if (element == null)
			return;
		var addAction = new AddLibraryAction();
		addAction.accept(Collections.singletonList(Navigator.findElement(Database.get())));
		addAction.setCallback(added -> {
			if (added.isEmpty())
				return;
			var lib = element.getContent();
			App.runWithProgress("Removing library " + lib.name() + " ...",
					() -> new Unmounter(Database.get()).unmountUnsafe(lib),					
					() -> Navigator.refresh());
		});
		addAction.run();
	}

}
