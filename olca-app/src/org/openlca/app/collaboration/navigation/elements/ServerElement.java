package org.openlca.app.collaboration.navigation.elements;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationElement;
import org.openlca.collaboration.api.CollaborationServer;

public class ServerElement extends NavigationElement<CollaborationServer>
		implements IRepositoryNavigationElement<CollaborationServer> {

	public ServerElement(INavigationElement<?> parent, CollaborationServer content) {
		super(parent, content);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var children = new ArrayList<INavigationElement<?>>();
		WebRequests.execute(
				() -> getContent().listRepositories().stream()
						.map(repo -> new RepositoryElement(this, repo))
						.forEach(children::add));
		return children;
	}

}
