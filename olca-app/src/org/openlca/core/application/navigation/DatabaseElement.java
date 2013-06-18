package org.openlca.core.application.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.application.db.Database;
import org.openlca.core.application.db.IDatabaseConfiguration;
import org.openlca.core.model.ModelType;

public class DatabaseElement implements INavigationElement {

	private IDatabaseConfiguration config;

	public DatabaseElement(IDatabaseConfiguration config) {
		this.config = config;
	}

	@Override
	public List<INavigationElement> getChildren() {
		if (!Database.isActive(config))
			return Collections.emptyList();
		//@formatter:off
		ModelType[] types = new ModelType[] {
			ModelType.PROJECT,
			ModelType.PRODUCT_SYSTEM,
			ModelType.IMPACT_METHOD,
			ModelType.PROCESS,
			ModelType.FLOW,
			ModelType.FLOW_PROPERTY,
			ModelType.UNIT_GROUP,
			ModelType.ACTOR,
			ModelType.SOURCE
		};
		//@formatter:on
		List<INavigationElement> elements = new ArrayList<>();
		for (ModelType type : types)
			elements.add(new ModelTypeElement(type));
		return elements;
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
