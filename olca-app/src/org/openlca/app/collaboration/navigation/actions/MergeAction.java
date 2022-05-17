package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitMerge;
import org.openlca.git.find.Commits;
import org.openlca.git.util.Constants;

public class MergeAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.Merge + "...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.MERGE.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return !Repository.get().history.getBehind().isEmpty();
	}

	@Override
	public void run() {
		Database.getWorkspaceIdUpdater().disable();
		var repo = Repository.get();
		var commits = Commits.of(repo.git);
		try {
			var remoteCommit = commits.get(commits.resolve(Constants.REMOTE_BRANCH));
			var conflictResolver = Conflicts.resolve(remoteCommit, false);
			if (conflictResolver == null)
				return;
			var changed = Actions.run(GitMerge
					.from(repo.git)
					.into(Database.get())
					.update(repo.workspaceIds)
					.as(repo.personIdent())
					.resolveConflictsWith(conflictResolver));
			if (!changed) {
				MsgBox.info("No changes to merge");
				return;
			}
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error during git merge", e);
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
		return true;
	}

}
