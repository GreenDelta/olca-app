package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.MsgBox;
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
			try {
				var repo = Repository.initialize(gitDir);
				repo.user(dialog.user());
			} catch (WebRequestException e) {
				MsgBox.error("Could not connect, is this an older version of the collaboration server?");
				Dirs.delete(gitDir);
				return;
			}
			Announcements.check();
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
