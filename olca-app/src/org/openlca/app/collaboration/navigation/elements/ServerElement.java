package org.openlca.app.collaboration.navigation.elements;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationElement;
import org.openlca.collaboration.client.CSClient;

public class ServerElement extends NavigationElement<ServerConfig>
		implements IRepositoryNavigationElement<ServerConfig> {

	protected final CSClient server;

	public ServerElement(INavigationElement<?> parent, ServerConfig content) {
		super(parent, content);
		this.server = content.open();
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		if (!ServerConfigurations.isActive(getContent()))
			return Collections.emptyList();
		var repositories = WebRequests.execute(server::listRepositories);
		if (repositories == null) {
			ServerConfigurations.deactivate(getContent());
			return Collections.emptyList();
		}
		return repositories.stream()
				.map(repo -> new RepositoryElement(this, repo))
				.collect(Collectors.toList());
	}

}
