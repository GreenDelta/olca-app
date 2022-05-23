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
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.collaboration.dialogs.RestrictionDialog;
import org.openlca.app.collaboration.navigation.RepositoryLabel;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
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
import org.openlca.git.model.Diff;
import org.openlca.git.util.Diffs;
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
		doRun(true);
	}

	boolean doRun(boolean canPush) {
		try {
			var repo = Repository.get();
			var committer = repo.promptCommitter();
			if (committer == null)
				return false;
			var credentials = Actions.credentialsProvider();
			var diffs = Diffs.workspace(repo.toConfig());
			var dialog = createCommitDialog(diffs, canPush);
			if (dialog == null)
				return false;
			var dialogResult = dialog.open();
			if (dialogResult == CommitDialog.CANCEL)
				return false;
			var withReferences = dialog.getSelected();
			// TODO new ReferenceCheck(Database.get()).run(dialog.getSelected(),
			// changes);
			if (withReferences == null)
				return false;
			if (!checkRestrictions(withReferences))
				return false;
			Actions.run(GitCommit.from(Database.get())
					.to(repo.git)
					.changes(withReferences.stream().map(d -> new Change(d.leftDiffType, d))
							.collect(Collectors.toList()))
					.withMessage(dialog.getMessage())
					.as(committer)
					.update(repo.workspaceIds));
			if (dialogResult != CommitDialog.COMMIT_AND_PUSH)
				return true;
			var result = Actions.run(GitPush
					.from(Repository.get().git)
					.authorizeWith(credentials));
			if (result.status() == Status.REJECTED_NONFASTFORWARD) {
				MsgBox.error("Rejected - Not up to date - Please merge remote changes to continue");
				return false;
			} else {
				Collections.reverse(result.newCommits());
				new HistoryDialog("Pushed commits", result.newCommits()).open();
				return true;
			}
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error during commit", e);
			return false;
		} finally {
			Actions.refresh();
		}
	}

	private CommitDialog createCommitDialog(List<Diff> diffs, boolean canPush) {
		var differences = diffs.stream()
				.map(d -> new TriDiff(d, null))
				.toList();
		var node = new DiffNodeBuilder(Database.get()).build(differences);
		if (node == null) {
			MsgBox.info("No changes to commit");
			return null;
		}
		var dialog = new CommitDialog(node, canPush);
		var paths = PathFilters.of(selection);
		var initialSelection = new TypeRefIdSet();
		diffs.stream()
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

	private boolean checkRestrictions(List<TriDiff> refs) {
		if (!CollaborationPreference.checkRestrictions())
			return true;
		if (!Repository.get().isCollaborationServer())
			return true;
		try {
			var restricted = Repository.get().client.checkRestrictions(refs);
			if (restricted.isEmpty())
				return true;
			var code = new RestrictionDialog(restricted).open();
			return code == RestrictionDialog.OK;
		} catch (WebRequestException e) {
			Actions.handleException("Error performing restriction check", e);
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
