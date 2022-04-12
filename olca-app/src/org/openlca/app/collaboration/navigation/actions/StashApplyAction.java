package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
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
			if (!Actions.getWorkspaceChanges().isEmpty()) {
				// TODO allow if not conflicting
				MsgBox.info("You can only apply stashes to an unchanged database");
			}
			var stashCommit = Actions.getStashCommit(repo.git);
			if (stashCommit == null)
				return;
			var conflictResolutionMap = Conflicts.identifyAndSolve(stashCommit);
			if (conflictResolutionMap == null)
				return;
			GitStashApply.from(repo.git)
					.to(Database.get())
					.update(repo.workspaceIds)
					.resolveConflictsWith(conflictResolutionMap)
					.run();
		} catch (IOException | GitAPIException e) {
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
