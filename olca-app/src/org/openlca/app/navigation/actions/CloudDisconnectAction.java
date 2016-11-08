package org.openlca.app.navigation.actions;

import org.openlca.app.M;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.cloud.api.RepositoryClient;

class CloudDisconnectAction extends Action implements INavigationAction {

	private RepositoryClient client;

	@Override
	public String getText() {
		return M.DisconnectFromRepository;
	}

	@Override
	public void run() {
		Database.disconnect();
		Navigator.refresh(Navigator.getNavigationRoot());
		HistoryView.refresh();
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		client = Database.getRepositoryClient();
		if (client == null)
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
