package org.openlca.app.navigation.actions.cloud;

import org.openlca.app.M;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.cloud.index.Reindexing;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;

public class RebuildIndexAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.RebuildIndex;
	}

	@Override
	public void run() {
		App.runWithProgress(M.RebuildingIndex, Reindexing::execute);
		Navigator.refresh(Navigator.getNavigationRoot());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var client = Database.getRepositoryClient();
		return client != null;
	}

}