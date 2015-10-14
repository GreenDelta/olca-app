package org.openlca.app.cloud.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.core.database.IDatabase;

import com.greendelta.cloud.api.RepositoryConfig;

public class NavigationRoot implements INavigationElement<NavigationRoot> {

	private List<INavigationElement<?>> children;

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
			children = Collections.singletonList(
					new RepositoryElement(RepositoryConfig.loadFor(database)));
		else
			children = Collections.emptyList();
		return children;
	}

	@Override
	public NavigationRoot getContent() {
		return this;
	}

	@Override
	public void update() {
		children = null;
	}

}
