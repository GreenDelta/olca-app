package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.collaboration.dialogs.LibraryRestrictionDialog;
import org.openlca.app.collaboration.navigation.RepositoryLabel;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.DiffResult;
import org.openlca.app.collaboration.views.PathFilters;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitCommit;
import org.openlca.git.actions.GitPush;
import org.openlca.git.model.Change;
import org.openlca.git.model.ModelRef;
import org.openlca.git.util.DiffEntries;
import org.openlca.git.util.TypeRefIdSet;

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
		return RepositoryLabel.hasChanged(Navigator.findElement(Database.getActiveConfiguration()));
	}

	@Override
	public void run() {
		try {
			var repo = Repository.get();
			var committer = repo.personIdent();
			if (committer == null)
				return;
			var changes = DiffEntries.workspace(repo.toConfig());
			var dialog = createCommitDialog(changes);
			if (dialog == null)
				return;
			var dialogResult = dialog.open();
			if (dialogResult == CommitDialog.CANCEL)
				return;
			var withReferences = dialog.getSelected();
			// TODO new ReferenceCheck(Database.get()).run(dialog.getSelected(),
			// changes);
			if (withReferences == null)
				return;
			if (!checkLibraries(withReferences))
				return;
			Actions.run(GitCommit.from(Database.get())
					.to(repo.git)
					.changes(withReferences)
					.withMessage(dialog.getMessage())
					.as(committer)
					.update(repo.workspaceIds));
			if (dialogResult != CommitDialog.COMMIT_AND_PUSH)
				return;
			var result = Actions.run(GitPush
					.from(Repository.get().git)
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

	private CommitDialog createCommitDialog(List<DiffEntry> changes) {
		var differences = changes.stream()
				.map(DiffResult::new)
				.toList();
		var node = new DiffNodeBuilder(Database.get()).build(differences);
		if (node == null) {
			MsgBox.info("No changes to commit");
			return null;
		}
		var dialog = new CommitDialog(node);
		var paths = PathFilters.of(selection);
		var initialSelection = new TypeRefIdSet();
		changes.stream()
				.map(ModelRef::new)
				.filter(ref -> selectionContainsPath(paths, ref.path))
				.forEach(ref -> initialSelection.add(ref.type, ref.refId));
		dialog.setInitialSelection(initialSelection);
		return dialog;
	}

	private boolean selectionContainsPath(List<String> paths, String path) {
		if (paths.isEmpty())
			return true;
		for (var p : paths)
			if (path.startsWith(p))
				return true;
		return false;
	}

	private boolean checkLibraries(List<Change> changes) {
		if (!CollaborationPreference.checkAgainstLibraries())
			return true;
		if (!Repository.get().isCollaborationServer())
			return true;
		try {
			var restricted = Repository.get().client.performLibraryCheck(changes);
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
