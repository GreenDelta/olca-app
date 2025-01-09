package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

class ChangeUserAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.ChangeUser;
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.EDIT.descriptor();
	}

	@Override
	public void run() {
		
		var ident = AuthenticationDialog.promptUser(Repository.CURRENT.serverUrl, null);
		if (ident == null)
			return;
		Repository.CURRENT.user(ident.getName());
		Navigator.refresh();
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement elem))
			return false;
		if (!Database.isActive(elem.getContent()))
			return false;
		return Repository.isConnected();
	}

}
