package org.openlca.app.collaboration.navigation.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.CommitDialog;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.preferences.CollaborationPreferenceDialog;
import org.openlca.app.collaboration.util.PathFilters;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.MsgBox;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.TriDiff;

class Datasets {

	static DialogResult select(List<INavigationElement<?>> selection, List<Diff> diffs, boolean canPush,
			boolean isStashCommit) {
		if (CollaborationPreference.firstConfiguration()) {
			new CollaborationPreferenceDialog().open();
		}
		var dialog = createCommitDialog(selection, diffs, canPush, isStashCommit);
		if (dialog == null)
			return null;
		var dialogResult = dialog.open();
		if (dialogResult == CommitDialog.CANCEL)
			return null;
		var withReferences = isStashCommit
				? ReferenceCheck.forStash(diffs, dialog.getSelected())
				: ReferenceCheck.forRemote(diffs, dialog.getSelected());
		if (withReferences == null)
			return null;
		var libraryAdditions = getLibraryAdditions(withReferences);
		withReferences.addAll(getLibraryDatasets(diffs, libraryAdditions));
		return new DialogResult(dialogResult, dialog.getMessage(), withReferences);
	}

	private static CommitDialog createCommitDialog(List<INavigationElement<?>> selection, List<Diff> diffs,
			boolean canPush, boolean isStashCommit) {
		var differences = diffs.stream()
				.filter(Datasets::isForeground)
				.map(d -> new TriDiff(d, null))
				.toList();
		var node = new DiffNodeBuilder(Database.get()).build(differences);
		if (node == null) {
			MsgBox.info(M.NoChangesToCommit);
			return null;
		}
		var dialog = new CommitDialog(node, canPush, isStashCommit);
		var paths = PathFilters.of(selection);
		var initialSelection = diffs.stream()
				.filter(ref -> selectionContainsPath(paths, ref.path))
				.map(ref -> ref.path)
				.collect(Collectors.toSet());
		dialog.setInitialSelection(initialSelection);
		return dialog;
	}

	static boolean isForeground(Diff diff) {
		var descriptors = Repository.CURRENT.descriptors;
		if (diff.isDataset)
			return !descriptors.isFromLibrary(diff);
		if (diff.isCategory) {
			var c = descriptors.getCategory(diff.path);
			return !descriptors.isOnlyInLibraries(c);
		}
		return true;
	}

	private static Set<String> getLibraryAdditions(List<Diff> selected) {
		return selected.stream()
				.filter(d -> d.isLibrary && d.diffType == DiffType.ADDED)
				.map(d -> d.name)
				.collect(Collectors.toSet());
	}

	private static List<Diff> getLibraryDatasets(List<Diff> diffs, Set<String> libraryAdditions) {
		if (libraryAdditions.isEmpty())
			return new ArrayList<>();
		var descriptors = Repository.CURRENT.descriptors;
		return diffs.stream().filter(diff -> {
			if (diff.isDataset && libraryAdditions.contains(descriptors.getLibrary(diff)))
				return true;
			if (diff.isCategory && descriptors.isOnlyInLibraries(descriptors.getCategory(diff.path), libraryAdditions))
				return true;
			return false;
		}).collect(Collectors.toList());
	}

	private static boolean selectionContainsPath(List<String> paths, String path) {
		if (paths.isEmpty())
			return true;
		for (var p : paths)
			if (path.startsWith(p) || p.startsWith(path + "/"))
				return true;
		return false;
	}

	static record DialogResult(int action, String message, List<Diff> datasets) {
	}

}
