package org.openlca.core.application.navigation;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.application.db.Database;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IRootEntityDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class ModelTypeElement implements INavigationElement {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ModelType modelType;

	public ModelTypeElement(ModelType modelType) {
		this.modelType = modelType;
	}

	@Override
	public List<INavigationElement> getChildren() {
		List<INavigationElement> childs = new ArrayList<>();
		addCategoryElements(childs);
		addModelElements(childs);
		return childs;
	}

	private void addCategoryElements(List<INavigationElement> elements) {
		try {
			CategoryDao dao = new CategoryDao(Database.getEntityFactory());
			for (Category category : dao.getRootCategories(modelType)) {
				elements.add(new CategoryElement(this, category));
			}
		} catch (Exception e) {
			log.error("failed to add category elements: " + modelType, e);
		}
	}

	private void addModelElements(List<INavigationElement> elements) {
		try {
			IRootEntityDao<?> entityDao = Database.createRootDao(modelType);
			if (entityDao == null)
				return;
			Optional<Category> nil = Optional.absent();
			for (BaseDescriptor descriptor : entityDao.getDescriptors(nil)) {
				elements.add(new ModelElement(this, descriptor));
			}
		} catch (Exception e) {
			log.error("Failed to add model elements: " + modelType, e);
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
