package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitMerge;
import org.openlca.git.actions.GitMerge.MergeResultType;
import org.openlca.git.util.Constants;

class Merge {

	static void on(Repository repo, PersonIdent user, boolean silent)
			throws IOException, InvocationTargetException, InterruptedException, GitAPIException {
		var stashCommitBefore = repo.commits.stash();
		var dependencyResolver = WorkspaceDepencencyResolver.forRemote(repo);
		if (dependencyResolver == null)
			return;
		var conflictResolutions = ConflictResolver.resolve(repo, Constants.REMOTE_REF);
		if (conflictResolutions == null)
			return;
		if (!repo.localHistory.getAheadOf(Constants.REMOTE_REF).isEmpty()) {
			user = repo.promptUser();
			if (user == null)
				return;
		}
		var mergeResult = Actions.run(GitMerge
				.on(repo)
				.into(repo.dataPackage)
				.as(user)
				.resolveConflictsWith(conflictResolutions)
				.resolveDependenciesWith(dependencyResolver));
		if (mergeResult.type() == MergeResultType.ABORTED)
			return;
		var stashCommitAfter = repo.commits.stash();
		if (stashCommitAfter != null && !stashCommitAfter.equals(stashCommitBefore)) {
			Stash.askToApply();
		}
		if (mergeResult.type() == MergeResultType.MOUNT_ERROR) {
			MsgBox.error(M.CouldNotMountLibrary);
		} else if (mergeResult.type() == MergeResultType.NO_CHANGES && !silent) {
			MsgBox.info(M.NoChangesToMerge);
		}
	}

}
