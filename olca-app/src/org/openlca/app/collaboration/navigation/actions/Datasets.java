package org.openlca.app.collaboration.navigation.actions;

import java.util.List;
import java.util.Set;

import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.dialogs.RestrictionDialog;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.PathFilters;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.MsgBox;
import org.openlca.core.database.Daos;
import org.openlca.git.model.Diff;
import org.openlca.git.util.Diffs;
import org.openlca.git.util.TypedRefId;
import org.openlca.git.util.TypedRefIdSet;
import org.openlca.util.Strings;

class Datasets {

	static DialogResult select(List<INavigationElement<?>> selection, boolean canPush, boolean isStashCommit) {
		var repo = Repository.get();
		var diffs = Diffs.of(repo.git).with(Database.get(), repo.gitIndex);
		var dialog = createCommitDialog(selection, diffs, canPush, isStashCommit);
		if (dialog == null)
			return null;
		var dialogResult = dialog.open();
		if (dialogResult == CommitDialog.CANCEL)
			return null;
		var withReferences = isStashCommit
				? ReferenceCheck.forStash(Database.get(), diffs, dialog.getSelected())
				: ReferenceCheck.forRemote(Database.get(), diffs, dialog.getSelected());
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
		var newLibraryDatasets = determineLibraryDatasets(diffs);
		var initialSelection = new TypedRefIdSet();
		diffs.stream()
				.filter(ref -> selectionContainsPath(paths, ref.path) || newLibraryDatasets.contains(ref))
				.forEach(initialSelection::add);
		dialog.setInitialSelection(initialSelection);
		dialog.setNewLibraryDatasets(newLibraryDatasets);
		return dialog;
	}

	private static TypedRefIdSet determineLibraryDatasets(List<Diff> diffs) {
		var all = new TypedRefIdSet();
		diffs.forEach(all::add);
		var fromLibrary = new TypedRefIdSet();
		all.types().forEach(type -> {
			fromLibrary.addAll(Daos.root(Database.get(), type).getDescriptors().stream()
					.filter(d -> !Strings.nullOrEmpty(d.library))
					.map(d -> new TypedRefId(d.type, d.refId))
					.filter(all::contains).toList());
		});
		return fromLibrary;
	}

	private static boolean selectionContainsPath(List<String> paths, String path) {
		if (paths.isEmpty())
			return true;
		for (var p : paths)
			if (path.startsWith(p))
				return true;
		return false;
	}

	private static boolean checkRestrictions(Set<TriDiff> refs) {
		if (!CollaborationPreference.checkRestrictions())
			return true;
		if (!Repository.get().isCollaborationServer())
			return true;
		var restricted = Repository.get().client.checkRestrictions(refs);
		if (restricted.isEmpty())
			return true;
		var code = new RestrictionDialog(restricted).open();
		return code == RestrictionDialog.OK;
	}

	static record DialogResult(int action, String message, Set<TriDiff> datasets) {
	}

}
