package org.openlca.app.collaboration.navigation.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.util.Constants;

public class FetchAction extends Action implements INavigationAction {

	private static final String NOTHING_TO_FETCH = "Remote does not have " + Constants.LOCAL_REF
			+ " available for fetch.";

	@Override
	public String getText() {
		return M.Fetch;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.FETCH.descriptor();
	}

	@Override
	public void run() {
		var repo = Repository.get();
		try {
			var credentials = AuthenticationDialog.promptCredentials(repo);
			if (credentials == null)
				return;
			var newCommits = Actions.run(credentials,
					GitFetch.to(repo.git));
			if (newCommits == null)
				return;
			if (newCommits.isEmpty()) {
				MsgBox.info("No commits to fetch - Everything up to date");
			} else {
				new HistoryDialog("Fetched commits", newCommits).open();
			}
		} catch (GitAPIException | InvocationTargetException | InterruptedException e) {
			if (e instanceof TransportException && NOTHING_TO_FETCH.equals(e.getMessage())) {
				MsgBox.info("No commits to fetch - Everything up to date");
			} else {
				Actions.handleException("Error fetching from remote", e);
			}
		} finally {
			Actions.refresh();
		}

	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
