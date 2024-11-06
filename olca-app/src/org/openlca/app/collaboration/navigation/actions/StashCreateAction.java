package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.navigation.NavCache;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.actions.GitStashCreate;

class StashCreateAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public String getText() {
		return M.Create;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.STASH_CREATE.descriptor();
	}

	@Override
	public boolean isEnabled() {
		try {
			var repo = Repository.CURRENT;
			if (repo.commits.stash() != null)
				return false;
			return NavCache.get().hasChanges();
		} catch (GitAPIException e) {
			return false;
		}
	}

	@Override
	public void run() {
		var repo = Repository.CURRENT;
		try {
			var input = Datasets.select(selection, false, true);
			if (input == null)
				return;
			var user = repo.promptUser();
			if (user == null)
				return;
			Actions.run(GitStashCreate.on(repo)
					.as(user)
					.resolveLibrariesWith(WorkspaceLibraryResolver.forCommit(repo.commits.head()))
					.changes(input.datasets()));
		} catch (IOException | InvocationTargetException | InterruptedException | GitAPIException e) {
			Actions.handleException("Error stashing changes", e);
		} finally {
			Cache.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!Repository.isConnected())
			return false;
		this.selection = selection;
		return true;
	}

}
