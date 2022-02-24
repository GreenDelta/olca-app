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
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.collaboration.dialogs.LibraryRestrictionDialog;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.DiffResult;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitCommit;
import org.openlca.git.actions.GitPush;
import org.openlca.git.model.Diff;
import org.openlca.git.util.DiffEntries;

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
	public void run() {
		try {
			var diffs = getWorkspaceDiffs();
			var dialog = createCommitDialog(diffs);
			if (dialog == null)
				return;
			var dialogResult = dialog.open();
			if (dialogResult == CommitDialog.CANCEL)
				return;
			var withReferences = new ReferenceCheck(Database.get()).run(dialog.getSelected(), diffs);
			if (withReferences == null)
				return;
			if (!checkLibraries(withReferences))
				return;
			var toCommit = withReferences.stream()
					.map(r -> r.local)
					.toList();
			GitCommit.from(Database.get())
					.to(Repository.get().git)
					.diffs(toCommit)
					.withMessage(dialog.getMessage())
					.as(Repository.get().personIdent())
					.update(Repository.get().workspaceIds)
					.run();
			if (dialogResult != CommitDialog.COMMIT_AND_PUSH)
				return;
			var result = Actions.run(GitPush
					.to(Repository.get().git)
					.authorizeWith(Actions.credentialsProvider()));
			if (result.status() == Status.REJECTED_NONFASTFORWARD) {
				MsgBox.error("Rejected - Not up to date - Please merge remote changes to continue");
			} else {
				Collections.reverse(result.newCommits());
				new HistoryDialog("Pushed commits", result.newCommits()).open();
			}
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error during commit", e);
		} finally {
			Actions.refresh();
		}
	}

	private List<Diff> getWorkspaceDiffs() throws IOException {
		var commit = Repository.get().commits.head();
		var leftCommitId = commit != null ? commit.id : null;
		return DiffEntries.workspace(Repository.get().toConfig(), commit).stream()
				.map(e -> new Diff(e, leftCommitId, null))
				.toList();
	}

	private CommitDialog createCommitDialog(List<Diff> diffs) {
		var differences = diffs.stream()
				.map(d -> new DiffResult(d, null))
				.toList();
		var node = new DiffNodeBuilder(Database.get()).build(differences);
		if (node == null) {
			MsgBox.info("No changes to commit");
			return null;
		}
		var dialog = new CommitDialog(node);
		dialog.setInitialSelection(selection);
		return dialog;
	}

	private boolean checkLibraries(List<DiffResult> result) {
		if (!CollaborationPreference.checkAgainstLibraries())
			return true;
		if (!Repository.get().isCollaborationServer())
			return true;
		var refs = result.stream().map(r -> r.local.ref()).toList();
		try {
			var restricted = Repository.get().client.performLibraryCheck(refs);
			if (restricted.isEmpty())
				return true;
			var code = new LibraryRestrictionDialog(restricted).open();
			return code == LibraryRestrictionDialog.OK;
		} catch (WebRequestException e) {
			Actions.handleException("Error performing library check", e);
			return false;
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
