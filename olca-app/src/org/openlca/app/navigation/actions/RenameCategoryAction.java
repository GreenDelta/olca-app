package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rename a category via the navigation tree.
 */
class RenameCategoryAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private INavigationElement<?> element;

	public RenameCategoryAction() {
		setText(M.Rename);
		setImageDescriptor(Icon.CHANGE.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof CategoryElement))
			return false;
		CategoryElement e = (CategoryElement) element;
		category = e.getContent();
		this.element = element;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		InputDialog dialog = new InputDialog(UI.shell(), M.Rename,
				M.PleaseEnterANewName, category.getName(), null);
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		if (newName == null || newName.trim().isEmpty()) {
			Error.showBox(M.NameCannotBeEmpty);
			return;
		}
		doUpdate(newName);
	}

	private void doUpdate(String newName) {
		try {
			// updates of category names are treated as if the user would:
			// 1) create a new category
			// 2) move all contents
			// 3) delete the old category
			// the new category is added in the dao already
			if (Database.getIndexUpdater() != null) {
				Database.getIndexUpdater().delete(CloudUtil.toDataset(category));
			}
			category.setName(newName.trim());
			new CategoryDao(Database.get()).update(category);
			Navigator.refresh(element);
		} catch (final Exception e) {
			log.error("Update category failed", e);
		}
	}
}
