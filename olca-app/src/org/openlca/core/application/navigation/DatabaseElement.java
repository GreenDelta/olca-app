package org.openlca.core.application.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.core.application.db.IDatabaseConfiguration;

public class DatabaseElement implements INavigationElement {

	private IDatabaseConfiguration config;

	public DatabaseElement(IDatabaseConfiguration config) {
		this.config = config;
	}

	@Override
	public List<INavigationElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public Object getData() {
		return config;
	}

	@Override
	public INavigationElement getParent() {
		return null;
	}

}
