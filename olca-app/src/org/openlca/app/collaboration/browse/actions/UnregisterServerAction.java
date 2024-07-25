package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.collaboration.browse.ServerNavigator;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.ServerElement;
import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.rcp.images.Icon;

public class UnregisterServerAction extends Action implements IServerNavigationAction {

	private ServerConfig selected;

	public UnregisterServerAction() {
		setText("Remove Collaboration Server");
		setImageDescriptor(Icon.DELETE.descriptor());
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
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
		ServerNavigator.refresh();
	}

}
