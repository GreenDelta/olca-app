package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.RepositoryElement;
import org.openlca.app.collaboration.browse.elements.ServerElement;
import org.openlca.app.collaboration.navigation.actions.AddRepositoryAction;
import org.openlca.app.rcp.images.Icon;

public class AddAsRepositoryAction extends Action implements IServerNavigationAction {

	private String repoUrl;
	private String user;

	@Override
	public String getText() {
		return M.AddAsSubRepository;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.REPOSITORY.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return !Repository.isConnected(repoUrl);
	}

	@Override
	public void run() {
		AddRepositoryAction.connect(repoUrl, user);
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof RepositoryElement repoElem))
			return false;
		var serverConfig = ((ServerElement) repoElem.getParent()).getContent();
		this.repoUrl = serverConfig.url() + "/" + repoElem.getContent().id();
		this.user = serverConfig.user();
		return true;
	}

}
