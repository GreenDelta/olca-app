package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.util.Categories;

/**
 * This action creates a new category and appends it to the specified parent
 * category
 */
class CreateCategoryAction extends Action implements INavigationAction {

	private Category parent;
	private ModelType modelType;

	public CreateCategoryAction() {
		setText(M.AddNewChildCategory);
		setImageDescriptor(Icon.ADD.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		this.parent = null;
		this.modelType = null;

		if (first instanceof ModelTypeElement) {
			this.modelType = (ModelType) first.getContent();
		}
		if (first instanceof CategoryElement) {
			var category = (Category) first.getContent();
			parent = category;
			modelType = category.modelType;
		}

		// also, do not allow creation of categories in libraries
		return modelType != null && first.getLibrary().isEmpty();
	}

	@Override
	public void run() {
		if (modelType == null)
			return;
		var category = createCategory();
		if (category == null)
			return;
		try {
			category = tryInsert(category);
			if (category == null)
				return;
			// we have to refresh the category starting from its root
			// otherwise the object model is out of sync.
			var element = Navigator.findElement(category.modelType);
			Navigator.refresh(element);
			Navigator.select(category);
		} catch (Exception e) {
			ErrorReporter.on("failed to save category", e);
		}
	}

	private Category tryInsert(Category c) {
		var dao = new CategoryDao(Database.get());
		c.category = parent;
		c.refId = Categories.createRefId(c);
		if (parent == null)
			return dao.insert(c);
		parent.childCategories.add(c);
		parent = dao.update(parent);
		for (var child : parent.childCategories)
			if (c.refId.equals(child.refId))
				return child;
		return null;
	}

	private Category createCategory() {
		String name = getDialogValue();
		if (name == null || name.trim().isEmpty())
			return null;
		name = name.trim();
		var c = new Category();
		c.name = name;
		c.modelType = modelType;
		return c;
	}

	private String getDialogValue() {
		var dialog = new InputDialog(UI.shell(), M.NewCategory,
				M.PleaseEnterTheNameOfTheNewCategory,
				M.NewCategory, new CategoryNameValidator());
		int rc = dialog.open();
		if (rc == Window.OK)
			return dialog.getValue();
		return null;
	}

	static class CategoryNameValidator implements IInputValidator {

		@Override
		public String isValid(String newText) {
			if (newText != null && newText.contains("/"))
				return "The character / can not be used";
			return null;
		}

	}

}
