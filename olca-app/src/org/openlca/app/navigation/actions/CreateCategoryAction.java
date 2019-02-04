package org.openlca.app.navigation.actions;

import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.cloud.util.Datasets;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action creates a new category and appends it to the specified parent
 * category
 */
class CreateCategoryAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category parent;
	private ModelType modelType;

	public CreateCategoryAction() {
		setText(M.AddNewChildCategory);
		setImageDescriptor(Icon.ADD.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> e) {
		if (e instanceof ModelTypeElement) {
			ModelType type = (ModelType) e.getContent();
			this.parent = null;
			this.modelType = type;
			return true;
		}
		if (e instanceof CategoryElement) {
			Category category = (Category) e.getContent();
			parent = category;
			modelType = category.modelType;
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
			INavigationElement<?> element = Navigator.findElement(category.modelType);
			Navigator.refresh(element);
			Navigator.select(category);
		} catch (Exception e) {
			log.error("failed to save category", e);
		}
	}

	private void tryInsert(Category c) {
		CategoryDao dao = new CategoryDao(Database.get());
		if (parent == null)
			dao.insert(c);
		else {
			c.category = parent;
			parent.childCategories.add(c);
			dao.update(parent);
			// have to add to diff index manually here
			Database.getIndexUpdater().insert(
					Datasets.toDataset(c), c.id);
		}
	}

	private Category createCategory() {
		String name = getDialogValue();
		if (name == null || name.trim().isEmpty())
			return null;
		name = name.trim();
		Category c = new Category();
		c.name = name;
		c.refId = UUID.randomUUID().toString();
		c.modelType = modelType;
		return c;
	}

	private String getDialogValue() {
		InputDialog dialog = new InputDialog(UI.shell(), M.NewCategory,
				M.PleaseEnterTheNameOfTheNewCategory,
				M.NewCategory, null);
		int rc = dialog.open();
		if (rc == Window.OK)
			return dialog.getValue();
		return null;
	}

}
