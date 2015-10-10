package org.openlca.app.cloud.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.core.model.ModelType;

import com.greendelta.cloud.api.RepositoryConfig;

public class RepositoryElement implements INavigationElement<RepositoryConfig> {

	private RepositoryConfig config;
	private List<INavigationElement<?>> children;

	public RepositoryElement(RepositoryConfig config) {
		this.config = config;
	}

	@Override
	public INavigationElement<?> getParent() {
		return null;
	}

	@Override
	public List<INavigationElement<?>> getChildren() {
		if (children != null)
			return children;
		if (config == null) {
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
	public RepositoryConfig getContent() {
		return config;
	}

	@Override
	public void update() {
		children = null;
	}

}
