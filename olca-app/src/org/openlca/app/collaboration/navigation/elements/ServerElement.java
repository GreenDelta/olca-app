package org.openlca.app.collaboration.navigation.elements;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationElement;
import org.openlca.collaboration.api.CollaborationServer;
import org.openlca.collaboration.model.Repository;

public class ServerElement extends NavigationElement<ServerConfig>
		implements IRepositoryNavigationElement<ServerConfig> {

	protected final CollaborationServer server;
	
	public ServerElement(INavigationElement<?> parent, ServerConfig content) {
		super(parent, content);
		this.server = content.open();
	}

	@Override
	public boolean hasChildren() {
		return true; // avoid making a request unless user expands the element
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var children = new ArrayList<INavigationElement<?>>();
		WebRequests.execute(
				() -> server.listRepositories(), new ArrayList<Repository>()).stream()
				.map(repo -> new RepositoryElement(this, repo))
				.forEach(children::add);
		if (children.isEmpty())
			return null;
		return children;
	}

}
