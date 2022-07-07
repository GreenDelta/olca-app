package org.openlca.app.navigation.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;

public class ModelTypeElement extends NavigationElement<ModelType> {

	public ModelTypeElement(INavigationElement<?> parent, ModelType type) {
		super(parent, type);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var type = getContent();
		var db = Database.get();

		var list = new ArrayList<INavigationElement<?>>();
		var lib = getLibrary().orElse(null);

		// add root categories
		for (var root : new CategoryDao(db).getRootCategories(type)) {
			if (lib == null || CategoryElement.hasLibraryContent(root, lib)) {
				list.add(new CategoryElement(this, root));
			}
		}

		// models without category
		var dao = Daos.root(Database.get(), type);
		if (dao == null)
			return list;
		for (var d : dao.getDescriptors(Optional.empty())) {
			if (lib == null || Objects.equals(lib, d.library)) {
				list.add(new ModelElement(this, d));
			}
		}
		return list;
	}
}
