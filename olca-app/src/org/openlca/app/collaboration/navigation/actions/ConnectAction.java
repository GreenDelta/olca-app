package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.MsgBox;

class ConnectAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.ConnectDots;
	}

	@Override
	public void run() {
		var dialog = new ConnectDialog();
		if (dialog.open() == ConnectDialog.CANCEL)
			return;
		var url = dialog.url();
		if (Repository.isConnected(url)) {
			MsgBox.info(M.RepositoryAlreadyConnected);
			return;
		}
		var repo = Repository.initialize(Database.get(), url);
		if (repo != null) {
			repo.user(dialog.user());
			Announcements.check();
		}
		Actions.refresh();
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement dbElem))
			return false;
		if (!Database.isActive(dbElem.getContent()))
			return false;
		return Repository.get() == null;
	}

}
