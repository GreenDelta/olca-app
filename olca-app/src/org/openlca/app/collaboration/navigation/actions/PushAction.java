package org.openlca.app.collaboration.navigation.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.ServerNavigator;
import org.openlca.app.collaboration.browse.elements.RepositoryElement;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitPush;
import org.openlca.git.util.Constants;

class PushAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.Push;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.PUSH.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return !Repository.CURRENT.localHistory.getAheadOf(Constants.REMOTE_REF).isEmpty();
	}

	@Override
	public void run() {
		var credentials = Repository.CURRENT.promptCredentials();
		run(credentials);
	}

	boolean run(GitCredentialsProvider credentials) {
		if (credentials == null)
			return false;
		try {
			var repo = Repository.CURRENT;
			var result = Actions.run(credentials,
					GitPush.from(repo));
			if (result == null)
				return false;
			if (result.status() == Status.REJECTED_NONFASTFORWARD) {
				MsgBox.error(M.RejectedNotUpToDateErr);
				return false;
			}
			if (result.newCommits().isEmpty()) {
				MsgBox.info(M.NoCommitToPushInfo);
				return false;
			}
			Libraries.uploadTo(repo);
			Collections.reverse(result.newCommits());
			new HistoryDialog(M.PushedCommits, result.newCommits()).open();
			ServerNavigator.refresh(RepositoryElement.class, r -> r.id().equals(repo.id));
			return true;
		} catch (GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error pushing to remote", e);
			return false;
		} finally {
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
