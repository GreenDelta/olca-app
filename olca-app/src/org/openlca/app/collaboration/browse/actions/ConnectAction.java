package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.RepositoryElement;
import org.openlca.app.collaboration.browse.elements.ServerElement;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;

public class ConnectAction extends Action implements IServerNavigationAction {

	private String repoUrl;
	private String user;

	@Override
	public String getText() {
		return M.ConnectDots;
	}

	@Override
	public boolean isEnabled() {
		return Database.get() != null && Repository.get() == null;
	}
	
	@Override
	public void run() {
		var repo = Repository.initialize(Database.get(), repoUrl);
		if (repo == null)
			return;
		repo.user(user);
		Announcements.check();
		Navigator.refresh();
		HistoryView.refresh();
		CompareView.clear();
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof RepositoryElement repoElem))
			return false;
		var serverConfig = ((ServerElement) repoElem.getParent()).getContent();
		this.repoUrl = serverConfig.url() + "/" + repoElem.getContent().id();
		this.user = serverConfig.user();
		return true;
	}

}
