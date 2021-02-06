package org.openlca.app.navigation.actions.libraries;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.openlca.util.Strings;

/**
 * An action for converting a library category into a normal category of the
 * foreground system. This is done by just removing the library flag from that
 * category and recursively from the parent categories.
 */
public class UseLibraryCategoryAction extends Action implements INavigationAction {

	private List<Category> categories;

	public UseLibraryCategoryAction() {
		setText("Use in foreground system");
		setImageDescriptor(Icon.ACCEPT.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		if (elements == null || elements.isEmpty())
			return false;
		this.categories = elements.stream()
			.filter(e -> e instanceof CategoryElement)
			.map(e -> ((CategoryElement) e).getContent())
			.filter(c -> !Strings.nullOrEmpty(c.library))
			.collect(Collectors.toList());
		return !this.categories.isEmpty();
	}

	@Override
	public void run() {
		var dao = new CategoryDao(Database.get());
		for (var category : categories) {
			var c = category;
			while (c != null && c.library != null) {
				c.library = null;
				c = dao.update(c);
				c = c.category;
			}
		}
		Navigator.refresh();
	}
}
