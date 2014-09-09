package org.openlca.app.navigation.actions;

import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action creates a new category and appends it to the specified parent
 * category
 */
public class CreateCategoryAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category parent;
	private ModelType modelType;

	public CreateCategoryAction() {
		setText(Messages.AddNewChildCategory);
		setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (element instanceof ModelTypeElement) {
			ModelType type = (ModelType) element.getContent();
			this.parent = null;
			this.modelType = type;
			return true;
		}
		if (element instanceof CategoryElement) {
			Category category = (Category) element.getContent();
			parent = category;
			modelType = category.getModelType();
			return true;
		}
		return false;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		if (modelType == null)
			return;
		Category category = createCategory();
		if (category == null)
			return;
		try {
			tryInsert(category);
			// we have to refresh the category starting from it's root
			// otherwise the object model is out of sync.
			INavigationElement<?> element = Navigator.findElement(category
					.getModelType());
			Navigator.refresh(element);
			Navigator.select(category);
		} catch (Exception e) {
			log.error("failed to save category", e);
		}
	}

	private void tryInsert(Category category) {
		BaseDao<Category> dao = Database.get().createDao(Category.class);
		if (parent == null)
			dao.insert(category);
		else {
			category.setParentCategory(parent);
			parent.getChildCategories().add(category);
			dao.update(parent);
		}
	}

	private Category createCategory() {
		String name = getDialogValue();
		if (name == null || name.trim().isEmpty())
			return null;
		name = name.trim();
		Category category = new Category();
		category.setName(name);
		category.setRefId(UUID.randomUUID().toString());
		category.setModelType(modelType);
		return category;
	}

	private String getDialogValue() {
		InputDialog dialog = new InputDialog(UI.shell(),
				Messages.NewCategory,
				Messages.PleaseEnterTheNameOfTheNewCategory,
				Messages.NewCategory, null);
		int rc = dialog.open();
		if (rc == Window.OK)
			return dialog.getValue();
		return null;
	}

}
