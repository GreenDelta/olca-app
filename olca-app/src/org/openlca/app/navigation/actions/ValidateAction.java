package org.openlca.app.navigation.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.NavigationRoot;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.validation.ValidationView;

public class ValidateAction extends Action implements INavigationAction {

	private final Set<INavigationElement<?>> selection = new HashSet<>();

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
	public boolean accept(INavigationElement<?> elem) {
		selection.clear();
		if (forDB) {
			if (!(elem instanceof DatabaseElement))
				return false;
			DatabaseElement dbElem = (DatabaseElement) elem;
			IDatabaseConfiguration config = dbElem.getContent();
			if (!Database.isActive(config))
				return false;
			selection.add(elem);
			return true;
		} else if (elem instanceof DatabaseElement) {
			return false;
		}
		selection.add(elem);
		return !selection.isEmpty();
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		selection.clear();
		for (INavigationElement<?> element : elements) {
			if (!(element instanceof DatabaseElement))
				continue;
			DatabaseElement e = (DatabaseElement) element;
			IDatabaseConfiguration config = e.getContent();
			if (!Database.isActive(config))
				return false;
		}
		selection.addAll(elements);
		return !selection.isEmpty();
	}

	@Override
	public void run() {
		if (!selection.isEmpty()) {
			ValidationView.validate(selection);
			return;
		}
		if (!forDB)
			return;
		NavigationRoot root = Navigator.getNavigationRoot();
		for (INavigationElement<?> e : root.getChildren()) {
			if (!(e instanceof DatabaseElement))
				continue;
			DatabaseElement elem = (DatabaseElement) e;
			IDatabaseConfiguration config = elem.getContent();
			if (Database.isActive(config)) {
				selection.add(elem);
				break;
			}
		}
		if (!selection.isEmpty()) {
			ValidationView.validate(selection);
		}
	}

}
