package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseList;
import org.openlca.app.db.IDatabaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root element of the navigation tree: shows the database configurations.
 */
public class NavigationRoot extends PlatformObject implements
		INavigationElement<NavigationRoot> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private List<INavigationElement<?>> childs;

	@Override
	public NavigationRoot getContent() {
		return this;
	}

	@Override
	public void update() {
		childs = null;
	}

	@Override
	public List<INavigationElement<?>> getChildren() {
		if (childs == null)
			childs = loadChilds();
		return childs;
	}

	@Override
	public INavigationElement<?> getParent() {
		return null;
	}

	private List<INavigationElement<?>> loadChilds() {
		log.trace("create database navigation elements");
		DatabaseList list = Database.getConfigurations();
		List<INavigationElement<?>> elements = new ArrayList<>();
		for (IDatabaseConfiguration config : list.getDatabases())
			elements.add(new DatabaseElement(this, config));
		return elements;
	}

}
