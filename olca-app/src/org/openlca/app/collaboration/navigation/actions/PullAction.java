package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.actions.GitMerge;
import org.openlca.git.find.Commits;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Diffs;

public class PullAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.Pull + "...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.PULL.descriptor();
	}

	@Override
	public void run() {
		Database.getWorkspaceIdUpdater().disable();
		var repo = Repository.get();
		var commits = Commits.of(repo.git);
		try {
			var newCommits = Actions.run(GitFetch
					.to(repo.git)
					.authorizeWith(Actions.credentialsProvider()));
			if (!newCommits.isEmpty()) {
				new HistoryDialog("Fetched commits", newCommits).open();
			}
			if (!Diffs.workspace(repo.toConfig()).isEmpty()) {
				// TODO allow if not conflicting
				// TODO offer different solutions (e.g. stash, discard, commit)
				MsgBox.info("You can only merge into an unchanged database, please stash your changes first");
				return;
			}
			var remoteCommit = commits.get(commits.resolve(Constants.REMOTE_BRANCH));
			var conflictResolutionMap = Conflicts.identifyAndSolve(remoteCommit);
			if (conflictResolutionMap == null)
				return;
			var changed = Actions.run(GitMerge
					.from(repo.git)
					.into(Database.get())
					.update(repo.workspaceIds)
					.as(repo.personIdent())
					.resolveConflictsWith(conflictResolutionMap));
			if (!changed) {
				MsgBox.info("No commits to fetch - Everything up to date");
			}
		} catch (IOException | InvocationTargetException | InterruptedException | GitAPIException e) {
			Actions.handleException("Error pulling data", e);
		} finally {
			Database.getWorkspaceIdUpdater().enable();
			Cache.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
