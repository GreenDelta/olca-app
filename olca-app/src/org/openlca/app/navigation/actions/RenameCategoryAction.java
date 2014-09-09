package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.core.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rename a category via the navigation tree.
 */
public class RenameCategoryAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private INavigationElement<?> element;

	public RenameCategoryAction() {
		setText(Messages.Rename);
		setImageDescriptor(ImageType.CHANGE_ICON.getDescriptor());
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
		InputDialog dialog = new InputDialog(UI.shell(), Messages.Rename,
				Messages.PleaseEnterANewName, category.getName(), null);
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		if (newName == null || newName.trim().isEmpty()) {
			Error.showBox(Messages.NameCannotBeEmpty);
			return;
		}
		doUpdate(newName);
	}

	private void doUpdate(String newName) {
		try {
			category.setName(newName.trim());
			Database.get().createDao(Category.class).update(category);
			Navigator.refresh(element);
		} catch (final Exception e) {
			log.error("Update category failed", e);
		}
	}
}
