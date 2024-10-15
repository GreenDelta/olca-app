package org.openlca.app.collaboration.navigation.actions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.PathFilters;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.MsgBox;
import org.openlca.core.database.Daos;
import org.openlca.git.model.Change;
import org.openlca.git.model.Diff;
import org.openlca.git.util.ModelRefSet;
import org.openlca.git.util.TypedRefId;
import org.openlca.util.Strings;

class Datasets {

	static DialogResult select(List<INavigationElement<?>> selection, boolean canPush, boolean isStashCommit) {
		var repo = Repository.CURRENT;
		var diffs = repo.diffs.find().withDatabase();
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
		var result = withReferences.stream()
				.map(Change::of)
				.flatMap(Set::stream)
				.collect(Collectors.toSet());
		return new DialogResult(dialogResult, dialog.getMessage(), result);
	}

	private static CommitDialog createCommitDialog(List<INavigationElement<?>> selection, List<Diff> diffs,
			boolean canPush, boolean isStashCommit) {
		var differences = diffs.stream()
				.map(d -> new TriDiff(d, null))
				.toList();
		var node = new DiffNodeBuilder(Database.get()).build(differences);
		if (node == null) {
			MsgBox.info(M.NoChangesToCommit);
			return null;
		}
		var dialog = new CommitDialog(node, canPush, isStashCommit);
		var paths = PathFilters.of(selection);
		var lockedDatasets = determineLockedDatasets(diffs);
		var initialSelection = new ModelRefSet();
		diffs.stream()
				.filter(ref -> ref.type != null)
				.filter(ref -> selectionContainsPath(paths, ref.path) || lockedDatasets.contains(ref))
				.forEach(initialSelection::add);
		dialog.setInitialSelection(initialSelection);
		dialog.setLockedDatasets(lockedDatasets);
		return dialog;
	}

	private static ModelRefSet determineLockedDatasets(List<Diff> diffs) {
		var all = new ModelRefSet();
		diffs.stream()
				.filter(diff -> diff.type != null)
				.forEach(all::add);
		if (CollaborationPreference.onlyFullCommits())
			return all;
		var fromLibrary = new ModelRefSet();
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

	static record DialogResult(int action, String message, Set<Change> datasets) {
	}

}
