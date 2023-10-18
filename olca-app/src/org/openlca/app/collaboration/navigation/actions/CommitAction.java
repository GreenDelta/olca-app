package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.collaboration.navigation.NavRoot;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitCommit;
import org.openlca.git.actions.GitPush;
import org.openlca.git.model.Change;

public class CommitAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public String getText() {
		return M.Commit + "...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.COMMIT.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return NavRoot.get().hasChanges();
	}

	@Override
	public void run() {
		doRun(true);
	}

	boolean doRun(boolean canPush) {
		try {
			var repo = Repository.get();
			var input = Datasets.select(selection, canPush, false);
			if (input == null || input.action() == CommitDialog.CANCEL)
				return false;
			var doPush = input.action() == CommitDialog.COMMIT_AND_PUSH;
			var credentials = doPush ? AuthenticationDialog.promptCredentials(repo) : null;
			var user = doPush && credentials != null ? credentials.ident : AuthenticationDialog.promptUser(repo);
			if (credentials == null && user == null)
				return false;
			var changes = input.datasets().stream()
					.map(d -> new Change(d.leftDiffType, d))
					.collect(Collectors.toList());
			Actions.run(GitCommit.from(Database.get())
					.to(repo.git)
					.changes(changes)
					.withMessage(input.message())
					.as(user)
					.update(repo.gitIndex));
			if (input.action() != CommitDialog.COMMIT_AND_PUSH)
				return true;
			var result = Actions.run(credentials,
					GitPush.from(Repository.get().git));
			if (result == null)
				return false;
			if (result.status() == Status.REJECTED_NONFASTFORWARD) {
				MsgBox.error("Rejected - Not up to date - Please merge remote changes to continue");
				return false;
			}
			Collections.reverse(result.newCommits());
			new HistoryDialog("Pushed commits", result.newCommits()).open();
			return true;
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error during commit", e);
			return false;
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
