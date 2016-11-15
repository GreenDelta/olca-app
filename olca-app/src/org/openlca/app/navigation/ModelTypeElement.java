package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class ModelTypeElement extends NavigationElement<ModelType> {

	private Logger log = LoggerFactory.getLogger(getClass());

	public ModelTypeElement(INavigationElement<?> parent, ModelType type) {
		super(parent, type);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		ModelType type = getContent();
		log.trace("get model type childs: {}", type);
		List<INavigationElement<?>> childs = new ArrayList<>();
		addCategoryElements(type, childs);
		addModelElements(type, childs);
		return childs;
	}

	private void addCategoryElements(ModelType type,
			List<INavigationElement<?>> elements) {
		try {
			CategoryDao dao = new CategoryDao(Database.get());
			for (Category category : dao.getRootCategories(type)) {
				elements.add(new CategoryElement(this, category));
			}
		} catch (Exception e) {
			log.error("failed to add category elements: " + type, e);
		}
	}

	private void addModelElements(ModelType type,
			List<INavigationElement<?>> elements) {
		try {
			CategorizedEntityDao<?, ?> entityDao = Database.createCategorizedDao(type);
			if (entityDao == null)
				return;
			Optional<Category> nil = Optional.absent();
			for (CategorizedDescriptor descriptor : entityDao.getDescriptors(nil)) 
				elements.add(new ModelElement(this, descriptor));
		} catch (Exception e) {
			log.error("Failed to add model elements: " + type, e);
		}
	}

}
