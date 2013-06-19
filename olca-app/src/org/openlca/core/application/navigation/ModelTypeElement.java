package org.openlca.core.application.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.application.db.Database;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelTypeElement implements INavigationElement {

	private ModelType modelType;

	public ModelTypeElement(ModelType modelType) {
		this.modelType = modelType;
	}

	@Override
	public List<INavigationElement> getChildren() {
		CategoryDao dao = new CategoryDao(Database.getEntityFactory());
		try {
			List<INavigationElement> elems = new ArrayList<>();
			for (Category category : dao.getRootCategories(modelType)) {
				elems.add(new CategoryElement(this, category));
			}
			return elems;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to get categories for " + modelType, e);
			return Collections.emptyList();
		}
	}

	@Override
	public INavigationElement getParent() {
		return null;
	}

	@Override
	public Object getData() {
		return modelType;
	}

}
