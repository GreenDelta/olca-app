package org.openlca.app.collaboration.ui.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.openlca.app.collaboration.api.RepositoryConfig;
import org.openlca.app.collaboration.ui.Announcements;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.Input;

public class ConnectAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "Connect...";
	}

	@Override
	public void run() {
		var url = Input.promptString("Connect to git repository", "Please enter the git URL to connect to", "");
		try {
			var git = Git.init().setBare(true).setGitDir(RepositoryConfig.getGirDir(Database.get())).call();
			git.remoteAdd().setName("origin").setUri(new URIish(url)).call();
			Repository.connect(Database.get());
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
