package org.openlca.app.navigation.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseConfig;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryDirElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.navigation.elements.MappingDirElement;
import org.openlca.app.navigation.elements.MappingFileElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.validation.ValidationView;

public class ValidateAction extends Action implements INavigationAction {

	private final Set<INavigationElement<?>> elements = new HashSet<>();

	/**
	 * This field is used to allow a different order for non database elements
	 * in the navigation menu (we have two instances there: one for models and
	 * one for the complete database). Also, if the validation action is for a
	 * database and the selection is null, the validation selects the navigation
	 * element of the active databases (if there is any) to be the content of
	 * the validation (so that this action can be called from the application
	 * menu).
	 */
	private final boolean forDB;

	public static ValidateAction forDatabase() {
		return new ValidateAction(true);
	}

	public static ValidateAction forModel() {
		return new ValidateAction(false);
	}

	private ValidateAction(boolean onlyIfDbElement) {
		setText(M.Validate);
		setImageDescriptor(Icon.VALIDATE.descriptor());
		this.forDB = onlyIfDbElement;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		this.elements.clear();
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);

		// database elements: only if forDB & isActive
		if (first instanceof DatabaseElement) {
			if (!forDB)
				return false;
			var config = ((DatabaseElement) first).getContent();
			if (!Database.isActive(config))
				return false;
			elements.add(first);
			return true;
		}
		if (forDB)
			return false;

		// skip some types of elements
		var excludes = List.of(
				ScriptElement.class,
				LibraryDirElement.class,
				LibraryElement.class,
				MappingDirElement.class,
				MappingFileElement.class);
		for (var exclude : excludes) {
			if (exclude.isAssignableFrom(first.getClass()))
				return false;
		}
		if (first.getLibrary().isPresent())
			return false;

		// model elements, categories etc.
		elements.add(first);
		return true;
	}

	@Override
	public void run() {
		if (!elements.isEmpty()) {
			ValidationView.validate(elements);
			return;
		}
		if (!forDB)
			return;
		NavigationRoot root = Navigator.getNavigationRoot();
		for (INavigationElement<?> e : root.getChildren()) {
			if (!(e instanceof DatabaseElement))
				continue;
			DatabaseElement elem = (DatabaseElement) e;
			DatabaseConfig config = elem.getContent();
			if (Database.isActive(config)) {
				elements.add(elem);
				break;
			}
		}
		if (!elements.isEmpty()) {
			ValidationView.validate(elements);
		}
	}

}
