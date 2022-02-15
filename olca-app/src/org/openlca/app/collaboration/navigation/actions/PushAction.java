package org.openlca.app.collaboration.navigation.actions;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.collaboration.util.Constants;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;

public class PushAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "Push";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.PUSH.descriptor();
	}

	@Override
	public void run() {
		try {
			var commits = Repository.get().commits;
			var localCommitId = commits.resolve(Constants.LOCAL_BRANCH);
			var remoteCommitId = commits.resolve(Constants.REMOTE_BRANCH);
			var newCommits = commits.find()
					.after(remoteCommitId)
					.until(localCommitId)
					.all();
			if (newCommits.isEmpty()) {
				MsgBox.info("No commits to push - Everything up to date");
				return;
			}
			var git = Git.wrap(Repository.get().git);
			git.gc().call();
			var gitPush = git.push()
					.setCredentialsProvider(Actions.credentialsProvider())
					.setRemote(Constants.DEFAULT_REMOTE)
					.setRefSpecs(new RefSpec(Constants.LOCAL_REF));
			var result = Actions.runWithProgress(monitor -> gitPush
					.setProgressMonitor(Actions.progressMonitor(monitor))
					.call());
			if (result == null)
				return;
			var update = getUpdate(result);
			if (update == null) {
				MsgBox.info("No commits to push - Everything up to date");
			} else if (update.getStatus() == Status.REJECTED_NONFASTFORWARD) {
				MsgBox.error("Rejected - Not up to date - Please merge remote changes to continue");
			} else {
				Collections.reverse(newCommits);
				new HistoryDialog("Pushed commits", newCommits).open();
			}
		} catch (GitAPIException e) {
			Actions.handleException("Error pushing to remote", e);
		} finally {
			Actions.refresh();
		}
	}

	private RemoteRefUpdate getUpdate(Iterable<PushResult> results) {
		var iterator = results.iterator();
		if (!iterator.hasNext())
			return null;
		var result = iterator.next();
		if (result == null)
			return null;
		return result.getRemoteUpdate(Constants.LOCAL_REF);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
