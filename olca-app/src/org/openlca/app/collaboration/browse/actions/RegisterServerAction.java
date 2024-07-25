package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.ServerElement;
import org.openlca.app.collaboration.dialogs.ServerWizard;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Images;

public class RegisterServerAction extends Action implements IServerNavigationAction {

	public RegisterServerAction() {
		setText(M.NewCollaborationServer);
		setImageDescriptor(Images.newServer());
	}

	@Override
	public void run() {
		ServerWizard.open();
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
		if (selection.isEmpty())
			return true;
		var first = selection.get(0);
		return first instanceof ServerElement
				|| first instanceof DatabaseElement
				|| first instanceof NavigationRoot;
	}

}
