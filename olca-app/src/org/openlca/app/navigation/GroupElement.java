package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.core.model.ModelType;

class GroupElement extends NavigationElement<IDatabaseConfiguration> {

	private ModelType[] types;
	public final String label;

	public GroupElement(INavigationElement<?> parent,
			IDatabaseConfiguration dbConfig, String label, ModelType... types) {
		super(parent, dbConfig);
		this.label = label;
		this.types = types;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		if (!Database.isActive(getContent()))
			return Collections.emptyList();
		List<INavigationElement<?>> elements = new ArrayList<>();
		for (ModelType type : types)
			elements.add(new ModelTypeElement(this, type));
		return elements;
	}

}
