package org.openlca.app.collaboration.ui.navigation.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.collaboration.ui.dialogs.HistoryDialog;
import org.openlca.app.collaboration.util.Constants;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.model.Commit;

public class FetchAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "Fetch";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.FETCH.descriptor();
	}

	List<Commit> fetch() throws GitAPIException {
		var commits = Repository.get().commits;
		var lastId = commits.find()
				.refs(Constants.REMOTE_REF)
				.latestId();
		var git = Git.wrap(Repository.get().git);
		var gitFetch = git.fetch()
				.setCredentialsProvider(Actions.credentialsProvider())
				.setRemote(Constants.DEFAULT_REMOTE)
				.setRefSpecs(Constants.DEFAULT_FETCH_SPEC);
		var result = Actions.runWithProgress(monitor -> gitFetch
				.setProgressMonitor(Actions.progressMonitor(monitor))
				.call());
		if (result == null)
			return new ArrayList<>();
		// TODO check if list is always correct
		var newCommits = commits.find()
				.refs(Constants.REMOTE_REF)
				.after(lastId)
				.all();
		Collections.reverse(newCommits);
		return newCommits;
	}

	void openHistoryDialog(List<Commit> newCommits) {
		new HistoryDialog("Fetched commits", newCommits).open();
	}

	void showNoCommitsMessage() {
		MsgBox.info("No commits to fetch - Everything up to date");
	}

	@Override
	public void run() {
		try {
			var newCommits = fetch();
			if (newCommits.isEmpty()) {
				showNoCommitsMessage();
			} else {
				openHistoryDialog(newCommits);
			}
		} catch (GitAPIException e) {
			Actions.handleException("Error fetching from remote", e);
		} finally {
			Actions.refresh();
		}

	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
