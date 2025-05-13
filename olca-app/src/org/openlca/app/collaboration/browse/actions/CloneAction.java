package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.RepositoryElement;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.navigation.actions.Clone;
import org.openlca.app.rcp.images.Icon;

class CloneAction extends Action implements IServerNavigationAction {

	private RepositoryElement elem;

	CloneAction() {
		setText(M.Clone);
		setImageDescriptor(Icon.CLONE.descriptor());
	}

	@Override
	public void run() {
		var serverUrl = elem.getClient().url;
		var url = serverUrl + "/" + elem.getRepositoryId();
		var credentials = AuthenticationDialog.promptCredentials(serverUrl);
		if (credentials == null)
			return;
		Clone.of(url, credentials.user, credentials.password);
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.getFirst();
		if (!(first instanceof RepositoryElement repoElem))
			return false;
		this.elem = repoElem;
		return true;
	}

}
