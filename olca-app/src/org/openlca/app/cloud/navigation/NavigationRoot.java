package org.openlca.app.cloud.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.core.database.IDatabase;

import com.greendelta.cloud.api.RepositoryClient;

public class NavigationRoot implements INavigationElement<NavigationRoot> {

	private List<INavigationElement<?>> children;
	private RepositoryClient client;

	public NavigationRoot() {

	}

	@Override
	public INavigationElement<?> getParent() {
		return null;
	}

	@Override
	public List<INavigationElement<?>> getChildren() {
		if (children != null)
			return children;
		IDatabase database = Database.get();
		if (database != null)
			children = Collections.singletonList(new RepositoryElement(client));
		else
			children = Collections.emptyList();
		return children;
	}

	@Override
	public NavigationRoot getContent() {
		return this;
	}

	public void setClient(RepositoryClient client) {
		this.client = client;
	}

	@Override
	public void update() {
		children = null;
	}

}
