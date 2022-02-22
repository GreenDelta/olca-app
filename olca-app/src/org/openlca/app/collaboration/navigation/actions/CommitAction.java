package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.collaboration.dialogs.CommitDialog;
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
import org.openlca.git.model.Diff;
import org.openlca.git.util.DiffEntries;

public class CommitAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public String getText() {
		return "Commit...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.COMMIT.descriptor();
	}

	@Override
	public void run() {
		try {
			var diffs = getWorkspaceDiffs();
			var input = getCommitInput(diffs);
			if (input == null)
				return;
			var withReferences = new ReferenceCheck(Database.get()).run(input.getSelected(), diffs);
			if (withReferences == null)
				return;
			if (!checkLibraries(withReferences))
				return;
			var toCommit = withReferences.stream()
					.map(r -> r.local)
					.toList();
			GitCommit
					.to(Repository.get().git)
					.diffs(toCommit)
					.withMessage(input.getMessage())
					.as(Repository.get().personIdent())
					.update(Repository.get().workspaceIds)
					.run();
		} catch (IOException e) {
			Actions.handleException("Error during commit", e);
		}
	}

	private List<Diff> getWorkspaceDiffs() throws IOException {
		var commit = Repository.get().commits.head();
		var leftCommitId = commit != null ? commit.id : null;
		return DiffEntries.workspace(Repository.get().toConfig(), commit).stream()
				.map(e -> new Diff(e, leftCommitId, null))
				.toList();
	}

	private CommitDialog getCommitInput(List<Diff> diffs) {
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
		if (dialog.open() != CommitDialog.OK)
			return null;
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
