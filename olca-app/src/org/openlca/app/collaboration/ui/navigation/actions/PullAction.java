package org.openlca.app.collaboration.ui.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.collaboration.util.Constants;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

public class PullAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "Pull...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.PULL.descriptor();
	}

	@Override
	public void run() {
		try {
			var fetchAction = new FetchAction();
			var newCommits = fetchAction.fetch();
			if (!newCommits.isEmpty()) {
				fetchAction.openHistoryDialog(newCommits);
			}
			var commits = Repository.get().commits;
			var localCommitId = commits.resolve(Constants.LOCAL_BRANCH);
			var remoteCommitId = commits.resolve(Constants.REMOTE_BRANCH);
			var mergeAction = new MergeAction();
			if (mergeAction.hasChanges(localCommitId, remoteCommitId)) {
				mergeAction.run();
			} else if (newCommits.isEmpty()) {
				fetchAction.showNoCommitsMessage();
			}
		} catch (GitAPIException e) {
			Actions.handleException("Error pulling data", e);
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
