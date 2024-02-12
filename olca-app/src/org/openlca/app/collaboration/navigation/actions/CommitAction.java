package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.collaboration.navigation.NavCache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.database.CategoryDao;
import org.openlca.git.actions.GitCommit;
import org.openlca.git.actions.GitPush;

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
		return NavCache.get().hasChanges();
	}

	@Override
	public void run() {
		doRun(true);
	}

	boolean doRun(boolean canPush) {
		try {
			if (!checkDatabase())
				return false;
			var repo = Repository.CURRENT;
			var input = Datasets.select(selection, canPush, false);
			if (input == null || input.action() == CommitDialog.CANCEL)
				return false;
			var doPush = input.action() == CommitDialog.COMMIT_AND_PUSH;
			var credentials = doPush ? AuthenticationDialog.promptCredentials(repo) : null;
			var user = doPush && credentials != null ? credentials.ident : AuthenticationDialog.promptUser(repo);
			if (credentials == null && user == null)
				return false;
			Actions.run(GitCommit.on(repo)
					.changes(input.datasets())
					.withMessage(input.message())
					.as(user));
			if (input.action() != CommitDialog.COMMIT_AND_PUSH)
				return true;
			var result = Actions.run(credentials,
					GitPush.from(Repository.CURRENT));
			if (result == null)
				return false;
			if (result.status() == Status.REJECTED_NONFASTFORWARD) {
				MsgBox.error("Rejected - Not up to date - Please pull remote changes to continue");
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

	private boolean checkDatabase() {
		var dao = new CategoryDao(Database.get());
		var withSlash = dao.getDescriptors().stream()
				.filter(c -> c.name.contains("/"))
				.toList();
		if (withSlash.isEmpty())
			return true;
		var message = "The following categories contain a slash (/) in their name, which is not allowed. A simple fix is to replace / with \\. Should this be done now?\r\n";
		for (var i = 0; i < Math.min(5, withSlash.size()); i++) {
			var category = dao.getForId(withSlash.get(i).id);
			message += "\r\n* " + category.name + " in " + Labels.plural(category.modelType);
			if (category.category != null) {
				message += "/" + category.category.toPath();
			}
		}
		if (withSlash.size() > 5) {
			message += "\r\n  and " + (withSlash.size() - 5) + " more";
		}
		if (!Question.ask("Invalid category names", message))
			return false;
		for (var descriptor : withSlash) {
			var category = dao.getForId(descriptor.id);
			category.name = category.name.replace("/", "\\");
			dao.update(category);
		}
		Repository.CURRENT.descriptors.reload();
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!Repository.isConnected())
			return false;
		this.selection = selection;
		return true;
	}

}
