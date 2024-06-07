package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.actions.GitStashDrop;

public class StashDropAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.Drop;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DELETE.descriptor();
	}

	@Override
	public boolean isEnabled() {
		var repo = Repository.CURRENT;
		try {
			return repo.commits.stash() != null;
		} catch (GitAPIException e) {
			return false;
		}
	}

	@Override
	public void run() {
		var repo = Repository.CURRENT;
		try {
			GitStashDrop.from(repo).run();
		} catch (IOException e) {
			Actions.handleException("Error dropping stash", e);
		} finally {
			Cache.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
