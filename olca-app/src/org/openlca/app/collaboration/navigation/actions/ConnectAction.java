package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.git.actions.GitInit;
import org.openlca.util.Dirs;

public class ConnectAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.Connect + "...";
	}

	@Override
	public void run() {
		var dialog = new ConnectDialog();
		if (dialog.open() == ConnectDialog.CANCEL)
			return;
		var url = dialog.url();
		var gitDir = Repository.gitDir(Database.get().getName());
		try {
			GitInit.in(gitDir).remoteUrl(url).run();
			var repo = Repository.initialize(gitDir);
			if (repo == null) {
				Dirs.delete(gitDir);
			} else {
				repo.user(dialog.user());
				Announcements.check();
			}
		} catch (Exception e) {
			Actions.handleException("Error connecting to repository", e);
		} finally {
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var elem = (DatabaseElement) first;
		if (!Database.isActive(elem.getContent()))
			return false;
		return !Repository.isConnected();
	}

}
