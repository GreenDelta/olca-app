package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;

class ImportAction extends Action implements INavigationAction {

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement dbElement = (DatabaseElement) element;
		return Database.isActive(dbElement.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
