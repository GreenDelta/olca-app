package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
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
		List<INavigationElement<?>> list = new ArrayList<>();
		list.add(new GroupElement(this, g("#Models",
				ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM,
				ModelType.IMPACT_METHOD,
				ModelType.PARAMETER)));
		list.add(new GroupElement(this, g("#Inventory",
				ModelType.PROCESS,
				ModelType.FLOW,
				ModelType.COST_CATEGORY,
				ModelType.SOCIAL_INDICATOR)));
		list.add(new GroupElement(this, g("#Background data",
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.ACTOR,
				ModelType.SOURCE,
				ModelType.LOCATION)));
		return list;
	}

	private Group g(String label, ModelType... types) {
		return new Group(label, types);
	}

}
