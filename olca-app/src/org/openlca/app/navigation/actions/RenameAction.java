package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.parameters.RenameParameterDialog;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RenameAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private INavigationElement<?> element;

	public RenameAction() {
		setText(M.Rename);
		setImageDescriptor(Icon.CHANGE.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof ModelElement
				|| element instanceof CategoryElement))
			return false;
		if (element.getLibrary().isPresent())
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

		// for parameters, open another dialog
		if (element instanceof ModelElement) {
			var d = ((ModelElement) element).getContent();
			if (d.type == ModelType.PARAMETER) {
				var param = new ParameterDao(Database.get())
						.getForId(d.id);
				RenameParameterDialog.open(param);
				return;
			}
		}

		var name = element instanceof CategoryElement
				? ((CategoryElement) element).getContent().name
				: ((ModelElement) element).getContent().name;

		var dialog = new InputDialog(UI.shell(), M.Rename,
				M.PleaseEnterANewName, name, null);
		if (dialog.open() != Window.OK)
			return;
		var newName = dialog.getValue();
		if (newName == null || newName.trim().isEmpty()) {
			MsgBox.error(M.NameCannotBeEmpty);
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
			category.name = newName.trim();
			new CategoryDao(Database.get()).update(category);
			Navigator.refresh(element);
		} catch (final Exception e) {
			log.error("Update category failed", e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends CategorizedEntity> void doUpdate(
			CategorizedDescriptor d, String newName) {
		var dao = (CategorizedEntityDao<T, ?>) Daos.categorized(
				Database.get(), d.type);
		T entity = dao.getForId(d.id);
		entity.name = newName.trim();
		dao.update(entity);
		Navigator.refresh(element.getParent());
	}

}
