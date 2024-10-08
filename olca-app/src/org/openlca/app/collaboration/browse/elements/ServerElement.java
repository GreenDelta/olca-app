package org.openlca.app.collaboration.browse.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.collaboration.client.CSClient;
import org.openlca.collaboration.model.LibraryInfo;
import org.openlca.collaboration.model.Repository;

public class ServerElement extends ServerNavigationElement<ServerConfig> {

	private static final Set<ServerConfig> ACTIVE = new HashSet<>();
	protected final CSClient client;

	public ServerElement(IServerNavigationElement<?> parent, ServerConfig content) {
		super(parent, content);
		this.client = content.createClient();
	}

	public void activate() {
		ACTIVE.add(getContent());
	}

	public boolean isActive() {
		return ACTIVE.contains(getContent());
	}

	@Override
	public boolean hasChildren() {
		if (!isActive())
			return false;
		return super.hasChildren();
	}

	@Override
	public List<IServerNavigationElement<?>> getChildren() {
		if (!isActive())
			return Collections.emptyList();
		return super.getChildren();
	}

	@Override
	protected List<IServerNavigationElement<?>> queryChildren() {
		var children = new ArrayList<IServerNavigationElement<?>>();
		WebRequests.execute(client::listRepositories, new ArrayList<Repository>()).stream()
				.map(repo -> new RepositoryElement(this, repo))
				.forEach(children::add);
		var libs = WebRequests.execute(client::listLibraries, new ArrayList<LibraryInfo>());
		if (!libs.isEmpty()) {
			children.add(new LibrariesElement(this, libs));
		}
		return children;
	}

}
