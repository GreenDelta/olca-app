package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.dialogs.RestrictionDialog;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.PathFilters;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.MsgBox;
import org.openlca.git.model.Diff;
import org.openlca.git.util.Diffs;
import org.openlca.git.util.TypeRefIdSet;

class Datasets {

	static DialogResult select(List<INavigationElement<?>> selection, boolean canPush, boolean isStashCommit) {
		var diffs = Diffs.workspace(Repository.get().toConfig());
		var dialog = createCommitDialog(selection, diffs, canPush, isStashCommit);
		if (dialog == null)
			return null;
		var dialogResult = dialog.open();
		if (dialogResult == CommitDialog.CANCEL)
			return null;
		var withReferences = new ReferenceCheck(Database.get())
				.run(dialog.getSelected(), diffs, isStashCommit);
		if (withReferences == null)
			return null;
		if (!checkRestrictions(withReferences))
			return null;
		return new DialogResult(dialogResult, dialog.getMessage(), withReferences);
	}

	private static CommitDialog createCommitDialog(List<INavigationElement<?>> selection, List<Diff> diffs,
			boolean canPush, boolean isStashCommit) {
		var differences = diffs.stream()
				.map(d -> new TriDiff(d, null))
				.toList();
		var node = new DiffNodeBuilder(Database.get()).build(differences);
		if (node == null) {
			MsgBox.info("No changes to commit");
			return null;
		}
		var dialog = new CommitDialog(node, canPush, isStashCommit);
		var paths = PathFilters.of(selection);
		var initialSelection = new TypeRefIdSet();
		diffs.stream()
				.filter(ref -> selectionContainsPath(paths, ref.path))
				.forEach(ref -> initialSelection.add(ref.type, ref.refId));
		dialog.setInitialSelection(initialSelection);
		return dialog;
	}

	private static boolean selectionContainsPath(List<String> paths, String path) {
		if (paths.isEmpty())
			return true;
		for (var p : paths)
			if (path.startsWith(p))
				return true;
		return false;
	}

	private static boolean checkRestrictions(List<TriDiff> refs) {
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

	static record DialogResult(int action, String message, List<TriDiff> datasets) {
	}

}
