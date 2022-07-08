package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.parameters.RenameParameterDialog;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.RootEntityDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

class RenameAction extends Action implements INavigationAction {

	private INavigationElement<?> element;

	public RenameAction() {
		setText(M.Rename);
		setImageDescriptor(Icon.CHANGE.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof ModelElement
			|| first instanceof CategoryElement)
			|| first.getLibrary().isPresent())
			return false;
		if (first instanceof ModelElement e) {
			if (e.isFromLibrary())
				return false;
		}
		if (first instanceof CategoryElement e) {
			if (e.hasLibraryContent())
				return false;
		}
		this.element = first;
		return true;
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
			category = new CategoryDao(Database.get()).update(category);
			Cache.evict(Descriptor.of(category));
			Navigator.refresh(element);
		} catch (final Exception e) {
			ErrorReporter.on("Failed to update category", e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends RootEntity> void doUpdate(
		RootDescriptor d, String newName) {
		var dao = (RootEntityDao<T, ?>) Daos.root(
			Database.get(), d.type);
		T entity = dao.getForId(d.id);
		entity.name = newName.trim();
		dao.update(entity);
		Navigator.refresh(element.getParent());
	}

}
