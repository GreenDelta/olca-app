package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.collaboration.navigation.elements.ServerElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

public class UnregisterServerAction extends Action implements INavigationAction {

	private ServerConfig selected;

	public UnregisterServerAction() {
		setText("Remove Collaboration Server");
		setImageDescriptor(Icon.DELETE.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.isEmpty())
			return false;
		var first = selection.get(0);
		if (!(first instanceof ServerElement e))
			return false;
		this.selected = e.getContent();
		return true;
	}

	@Override
	public void run() {
		ServerConfigurations.remove(selected);
		Navigator.refresh();
	}

}
