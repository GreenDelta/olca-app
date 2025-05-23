package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.navigation.actions.ConflictResolver.ConflictSolution;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitMerge;
import org.openlca.git.actions.GitMerge.MergeResult;
import org.openlca.git.util.Constants;

class MergeAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.MergeDots;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.MERGE.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return !Repository.CURRENT.localHistory.getBehindOf(Constants.REMOTE_REF).isEmpty();
	}

	@Override
	public void run() {
		var repo = Repository.CURRENT;
		try {
			if (!DatabaseCheck.isValid())
				return;
			var libraryResolver = WorkspaceLibraryResolver.forRemote();
			if (libraryResolver == null)
				return;
			var conflictResult = ConflictResolver.resolve(Constants.REMOTE_REF);
			if (conflictResult == null)
				return;
			var user = !repo.localHistory.getAheadOf(Constants.REMOTE_REF).isEmpty()
					? repo.promptUser()
					: null;
			if (user == null)
				return;
			var mergeResult = Actions.run(GitMerge
					.on(repo)
					.as(user)
					.resolveConflictsWith(conflictResult.resolutions())
					.resolveLibrariesWith(libraryResolver));
			if (mergeResult == MergeResult.ABORTED)
				return;
			if (conflictResult.solution() == ConflictSolution.STASHED) {
				Actions.askApplyStash();
			}
			if (mergeResult == MergeResult.MOUNT_ERROR) {
				MsgBox.error(M.CouldNotMountLibrary);
			} else if (mergeResult == MergeResult.NO_CHANGES) {
				MsgBox.info(M.NoChangesToMerge);
			}
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error during Git merge", e);
		} finally {
			AppContext.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
