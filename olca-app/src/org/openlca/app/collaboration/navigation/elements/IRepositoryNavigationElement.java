package org.openlca.app.collaboration.navigation.elements;

import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.collaboration.api.CollaborationServer;

public interface IRepositoryNavigationElement<T> extends INavigationElement<T> {

	default CollaborationServer getServer() {
		if (this instanceof ServerElement e)
			return e.server;
		if (getParent() instanceof IRepositoryNavigationElement e)
			return e.getServer();
		return null;
	}

	default String getRepositoryId() {
		if (this instanceof RepositoryElement e)
			return e.getContent().group() + "/" + e.getContent().name();
		if (getParent() instanceof IRepositoryNavigationElement e)
			return e.getRepositoryId();
		return null;
	}

}
