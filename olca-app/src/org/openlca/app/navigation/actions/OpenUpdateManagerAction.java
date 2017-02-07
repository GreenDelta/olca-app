package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.components.UpdateManager;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;

class OpenUpdateManagerAction extends Action implements INavigationAction {

	OpenUpdateManagerAction() {
		setText(M.OpenUpdateManager);
	}

	@Override
	public void run() {
		UpdateManager.openAll();
	}
	
	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement elem = (DatabaseElement) element;
		return Database.isActive(elem.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
