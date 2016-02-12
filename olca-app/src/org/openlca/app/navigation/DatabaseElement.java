package org.openlca.app.navigation;

import org.openlca.app.M;
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
		list.add(new GroupElement(this, g(M.Models, GroupType.MODELS,
				ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM,
				ModelType.IMPACT_METHOD,
				ModelType.PROCESS,
				ModelType.FLOW,
				ModelType.SOCIAL_INDICATOR, 
				ModelType.PARAMETER)));
		list.add(new GroupElement(this, g(M.BackgroundData, GroupType.BACKGROUND_DATA,
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.CURRENCY,
				ModelType.ACTOR,
				ModelType.SOURCE,
				ModelType.LOCATION)));
		return list;
	}

	private Group g(String label, GroupType type, ModelType... types) {
		return new Group(label, type, types);
	}

}
