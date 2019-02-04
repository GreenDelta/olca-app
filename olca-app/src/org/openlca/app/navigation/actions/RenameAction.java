package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.cloud.util.Datasets;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rename a category via the navigation tree.
 */
class RenameAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private INavigationElement<?> element;

	public RenameAction() {
		setText(M.Rename);
		setImageDescriptor(Icon.CHANGE.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof ModelElement || element instanceof CategoryElement))
			return false;
		this.element = element;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		String name = null;
		if (element instanceof CategoryElement) {
			name = ((CategoryElement) element).getContent().name;
		} else {
			name = ((ModelElement) element).getContent().name;
		}
		InputDialog dialog = new InputDialog(UI.shell(), M.Rename,
				M.PleaseEnterANewName, name, null);
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		if (newName == null || newName.trim().isEmpty()) {
			Error.showBox(M.NameCannotBeEmpty);
			return;
		}
		if (element instanceof CategoryElement) {
			doUpdate(((CategoryElement) element).getContent(), newName);
		} else {
			doUpdate(((ModelElement) element).getContent(), newName);
		}
	}

	private void doUpdate(Category category, String newName) {
		try {
			// updates of category names are treated as if the user would:
			// 1) create a new category
			// 2) move all contents
			// 3) delete the old category
			// the new category is added in the dao already
			if (Database.getIndexUpdater() != null) {
				Database.getIndexUpdater().delete(Datasets.toDataset(category));
			}
			category.name = newName.trim();
			new CategoryDao(Database.get()).update(category);
			Navigator.refresh(element);
		} catch (final Exception e) {
			log.error("Update category failed", e);
		}
	}

	private <T extends CategorizedEntity> void doUpdate(CategorizedDescriptor d, String newName) {
		@SuppressWarnings("unchecked")
		CategorizedEntityDao<T, ?> dao = (CategorizedEntityDao<T, ?>) Daos.categorized(Database.get(),
				d.type);
		T entity = dao.getForId(d.id);
		entity.name = newName.trim();
		entity = dao.update(entity);
		Navigator.refresh(element.getParent());
	}

}
