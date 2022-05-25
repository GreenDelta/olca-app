package org.openlca.app.collaboration.navigation.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitFetch;

public class FetchAction extends Action implements INavigationAction {

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
		try {
			var credentials = AuthenticationDialog.promptCredentials();
			if (credentials == null)
				return;
			var newCommits = Actions.run(credentials,
					GitFetch.to(Repository.get().git));
			if (newCommits == null)
				return;
			if (newCommits.isEmpty()) {
				MsgBox.info("No commits to fetch - Everything up to date");
			} else {
				new HistoryDialog("Fetched commits", newCommits).open();
			}
		} catch (GitAPIException | InvocationTargetException | InterruptedException e) {
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
