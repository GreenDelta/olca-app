package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.navigation.NavCache;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.actions.GitCommit;
import org.openlca.util.Strings;

class CommitAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public String getText() {
		return M.CommitDots;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.COMMIT.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return NavCache.get().hasChanges();
	}

	@Override
	public void run() {
		var repo = Repository.CURRENT;
		try {
			if (!DatabaseCheck.isValid())
				return;
			var diffs = repo.diffs.find().withDatabase();
			var input = Datasets.select(selection, diffs, true, false);
			if (input == null || input.action() == CommitDialog.CANCEL)
				return;
			var credentials = repo.promptCredentials();
			var user = credentials != null ? credentials.ident : repo.promptUser();
			if (credentials == null && user == null)
				return;
			var commitId = Actions.runWithCancel(GitCommit.on(repo)
					.changes(input.datasets())
					.withMessage(input.message())
					.as(user));
			if (Strings.isBlank(commitId))
				return;
			if (input.action() != CommitDialog.COMMIT_AND_PUSH)
				return;
			new PushAction().run(credentials);
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error during commit", repo.serverUrl, e);
		} finally {
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!Repository.isConnected())
			return false;
		this.selection = selection;
		return true;
	}

}
