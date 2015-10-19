package org.openlca.app.cloud.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.core.model.ModelType;

import org.openlca.cloud.api.RepositoryClient;

public class RepositoryElement implements INavigationElement<RepositoryClient> {

	private RepositoryClient client;
	private List<INavigationElement<?>> children;

	public RepositoryElement(RepositoryClient client) {
		this.client = client;
	}

	@Override
	public INavigationElement<?> getParent() {
		return null;
	}

	@Override
	public List<INavigationElement<?>> getChildren() {
		if (children != null)
			return children;
		if (client == null) {
			children = Collections.emptyList();
			return children;
		}
		ModelType[] types = new ModelType[] { ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM, ModelType.IMPACT_METHOD,
				ModelType.SOCIAL_INDICATOR, ModelType.PROCESS, ModelType.FLOW,
				ModelType.FLOW_PROPERTY, ModelType.UNIT_GROUP, ModelType.ACTOR,
				ModelType.SOURCE, ModelType.LOCATION, ModelType.PARAMETER, ModelType.COST_CATEGORY, ModelType.CURRENCY };
		children = new ArrayList<>();
		for (ModelType type : types)
			children.add(new ModelTypeElement(this, type));
		return children;
	}

	@Override
	public RepositoryClient getContent() {
		return client;
	}

	@Override
	public void update() {
		children = null;
	}

}
