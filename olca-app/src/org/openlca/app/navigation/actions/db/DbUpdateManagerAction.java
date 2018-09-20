package org.openlca.app.navigation.actions.db;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.UpdateManager;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;

public class DbUpdateManagerAction extends Action implements INavigationAction {

	public DbUpdateManagerAction() {
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
