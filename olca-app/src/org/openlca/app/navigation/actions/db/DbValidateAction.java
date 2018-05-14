package org.openlca.app.navigation.actions.db;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.validation.ValidationView;

public class DbValidateAction extends Action implements INavigationAction {

	private final Set<INavigationElement<?>> selection = new HashSet<>();

	public DbValidateAction() {
		setText(M.Validate);
		setImageDescriptor(Icon.VALIDATE.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		selection.clear();
		if (element instanceof DatabaseElement) {
			DatabaseElement e = (DatabaseElement) element;
			IDatabaseConfiguration config = e.getContent();
			if (!Database.isActive(config))
				return false;
		}
		selection.add(element);
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
		ValidationView.validate(selection);
	}

}
