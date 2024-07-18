package org.openlca.app.collaboration.browse.elements;

import java.util.List;

import org.openlca.collaboration.client.CSClient;

public interface IServerNavigationElement<T> {

	IServerNavigationElement<?> getParent();

	List<IServerNavigationElement<?>> getChildren();

	T getContent();

	void update();
	
	default boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	default CSClient getClient() {
		if (this instanceof ServerElement e)
			return e.client;
		if (getParent() instanceof IServerNavigationElement e)
			return e.getClient();
		return null;
	}

	default String getRepositoryId() {
		if (this instanceof RepositoryElement e)
			return e.getContent().group() + "/" + e.getContent().name();
		if (getParent() instanceof IServerNavigationElement e)
			return e.getRepositoryId();
		return null;
	}

}
