package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.viewers.Selections;

/**
 * Adds the actions to the context menu of the server navigation tree.
 */
public class ServerNavigationMenu extends CommonActionProvider {

	@Override
	public void fillContextMenu(IMenuManager menu) {
		var con = getContext();
		List<IServerNavigationElement<?>> selection = Selections.allOf(con.getSelection());
		addActions(selection, menu,
				new RegisterServerAction(),
				new CreateRepositoryAction(),
				new DownloadDatasetAction(),
				new DownloadLibraryAction(),
				new CloneAction(),
				new ConnectAction(),
				new AddAsRepositoryAction(),
				new DeleteRepositoryAction(),
				new UnregisterServerAction());
	}

	public static int addActions(
			List<IServerNavigationElement<?>> selection,
			IMenuManager menu,
			IServerNavigationAction... actions) {
		int count = 0;
		for (var action : actions) {
			if (action.accept(selection)) {
				menu.add(action);
				count++;
			}
		}
		if (count > 1) {
			menu.add(new Separator());
		}
		return count;
	}

}
