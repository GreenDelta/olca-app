package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.actions.GitMerge;
import org.openlca.git.actions.GitMerge.MergeResult;

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
	public boolean isEnabled() {
		return Repository.CURRENT.client != null;
	}

	@Override
	public void run() {
		var repo = Repository.CURRENT;
		try {
			var credentials = AuthenticationDialog.promptCredentials(repo);
			if (credentials == null)
				return;
			var newCommits = Actions.run(credentials,
					GitFetch.to(repo));
			if (newCommits == null)
				return;
			if (!newCommits.isEmpty()) {
				new HistoryDialog(M.FetchedCommits, newCommits).open();
			}
			var libraryResolver = WorkspaceLibraryResolver.forRemote();
			if (libraryResolver == null)
				return;
			var conflictResult = ConflictResolutionMap.forRemote();
			if (conflictResult == null)
				return;
			var mergeResult = Actions.run(GitMerge
					.on(repo)
					.as(credentials.ident)
					.resolveConflictsWith(conflictResult.resolutions())
					.resolveLibrariesWith(libraryResolver));
			if (mergeResult == MergeResult.ABORTED)
				return;
			if (conflictResult.stashedChanges()) {
				Actions.askApplyStash();
			}
			if (mergeResult == MergeResult.MOUNT_ERROR) {
				MsgBox.error("Could not mount library");
			} else if (mergeResult == MergeResult.NO_CHANGES) {
				if (newCommits.isEmpty()) {
					MsgBox.info("No commits to fetch - Everything up to date");
				} else {
					MsgBox.info("No changes to merge");
				}
			}
		} catch (IOException | InvocationTargetException | InterruptedException | GitAPIException e) {
			Actions.handleException("Error pulling data", e);
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
