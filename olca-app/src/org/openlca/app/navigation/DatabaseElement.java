package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.application.db.Database;
import org.openlca.core.application.db.IDatabaseConfiguration;
import org.openlca.core.model.ModelType;

/** Navigation element for databases. */
public class DatabaseElement extends NavigationElement<IDatabaseConfiguration> {

	public DatabaseElement(INavigationElement<?> parent,
			IDatabaseConfiguration config) {
		super(parent, config);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		if (!Database.isActive(getContent()))
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
		List<INavigationElement<?>> elements = new ArrayList<>();
		for (ModelType type : types)
			elements.add(new ModelTypeElement(this, type));
		return elements;
	}

}
