package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.collaboration.navigation.actions.ConflictResolver.ConflictSolution;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.actions.GitMerge;
import org.openlca.git.actions.GitMerge.MergeResult;
import org.openlca.git.util.Constants;

class PullAction extends Action implements INavigationAction {

	private final boolean silent;
	private Repository repo;

	PullAction() {
		this(false);
	}

	private PullAction(boolean silent) {
		this.silent = silent;
	}

	static PullAction silent() {
		var action = new PullAction(true);
		action.repo = Repository.get();
		return action;
	}

	public PullAction on(Repository repo) {
		this.repo = repo;
		return this;
	}

	@Override
	public String getText() {
		return M.PullDots;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.PULL.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void run() {
		try {
			var credentials = repo.promptCredentials();
			if (credentials == null)
				return;
			var newCommits = Actions.run(repo, credentials,
					GitFetch.to(repo));
			if (newCommits == null)
				return;
			if (!newCommits.isEmpty()) {
				new HistoryDialog(M.FetchedCommits, repo, newCommits).open();
			} else if (repo.localHistory.getBehindOf(Constants.REMOTE_REF).isEmpty()) {
				MsgBox.info(M.NoCommitToFetchInfo);
				return;
			}
			var libraryResolver = WorkspaceLibraryResolver.forRemote(repo);
			if (libraryResolver == null)
				return;
			var conflictResult = ConflictResolver.resolve(repo, Constants.REMOTE_REF);
			if (conflictResult == null)
				return;
			var mergeResult = Actions.run(GitMerge
					.on(repo)
					.into(repo.dataPackage)
					.as(credentials.ident)
					.resolveConflictsWith(conflictResult.resolutions())
					.resolveLibrariesWith(libraryResolver));
			if (mergeResult == MergeResult.ABORTED)
				return;
			if (conflictResult.solution() == ConflictSolution.STASHED) {
				Actions.askApplyStash();
			}
			if (mergeResult == MergeResult.MOUNT_ERROR) {
				MsgBox.error(M.CouldNotMountLibrary);
			} else if (mergeResult == MergeResult.NO_CHANGES && !silent) {
				if (newCommits.isEmpty()) {
					MsgBox.info(M.NoCommitToFetchInfo);
				} else {
					MsgBox.info(M.NoChangesToMerge);
				}
			}
		} catch (IOException | InvocationTargetException | InterruptedException | GitAPIException e) {
			if (e instanceof TransportException && FetchAction.NOTHING_TO_FETCH.equals(e.getMessage())) {
				if (!silent) {
					MsgBox.info("No commits to fetch - Everything up to date");
				}
			} else {
				Actions.handleException("Error pulling from remote", repo.serverUrl, e);
			}
		} finally {
			AppContext.evictAll();
			Actions.refresh();
			if (!silent && repo.dataPackage != null) {
				repo.close();
			}
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		repo = Actions.getRepo(selection);
		return repo != null;
	}

}
