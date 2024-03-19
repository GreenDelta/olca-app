package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.collaboration.dialogs.ServerWizard;
import org.openlca.app.collaboration.navigation.elements.ServerElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Images;

public class RegisterServerAction extends Action implements INavigationAction {

	public RegisterServerAction() {
		setText("New Collaboration Server");
		setImageDescriptor(Images.newServer());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.isEmpty())
			return true;
		var first = selection.get(0);
		return first instanceof ServerElement 
				|| first instanceof DatabaseElement
				|| first instanceof NavigationRoot;
	}

	@Override
	public void run() {
		ServerWizard.open();
	}
	
}
