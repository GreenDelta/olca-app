package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.library.Library;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;

public class ModelTypeElement extends NavigationElement<ModelType> {

	public ModelTypeElement(INavigationElement<?> parent, ModelType type) {
		super(parent, type);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		ModelType type = getContent();
		var db = Database.get();
		var lib = getLibrary().orElse(null);

		var childs = new ArrayList<INavigationElement<?>>();

		// add root categories
		new CategoryDao(db).getRootCategories(type).forEach(category -> {
			childs.add(new CategoryElement(this, category));
		});

		// models without category
		var dao = Daos.categorized(Database.get(), type);
		if (dao == null)
			return childs;
		for (var d : dao.getDescriptors(Optional.empty())) {
			if (matches(d, lib)) {
				childs.add(new ModelElement(this, d));
			}
		}
		return childs;
	}
}
