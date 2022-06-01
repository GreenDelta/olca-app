package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.actions.GitStashApply;

public class StashApplyAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "Apply";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.STASH_APPLY.descriptor();
	}

	@Override
	public boolean isEnabled() {
		try {
			var repo = Repository.get();
			return Actions.getStashCommit(repo.git) != null;
		} catch (GitAPIException e) {
			return false;
		}
	}

	@Override
	public void run() {
		Database.getWorkspaceIdUpdater().disable();
		var repo = Repository.get();
		try {
			var libraryResolver = WorkspaceLibraryResolver.forStash(repo.git);
			if (libraryResolver == null)
				return;
			var conflictResolver = Conflicts.resolve(Constants.R_STASH, true);
			if (conflictResolver == null)
				return;
			Actions.run(GitStashApply.from(repo.git)
					.to(Database.get())
					.update(repo.workspaceIds)
					.resolveConflictsWith(conflictResolver));
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error stashing changes", e);
		} finally {
			Database.getWorkspaceIdUpdater().enable();
			Cache.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		if (!Repository.isConnected())
			return false;
		return elements.size() == 1 && elements.get(0) instanceof DatabaseElement;
	}

}
