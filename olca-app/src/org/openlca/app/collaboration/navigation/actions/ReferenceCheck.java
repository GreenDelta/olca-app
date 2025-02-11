package org.openlca.app.collaboration.navigation.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.CommitReferencesDialog;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.db.Database;
import org.openlca.core.database.ModelReferences;
import org.openlca.core.model.TypedRefId;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.TriDiff;
import org.openlca.util.TypedRefIdMap;
import org.openlca.util.TypedRefIdSet;

class ReferenceCheck {

	private final TypedRefIdMap<Diff> diffs;
	private final List<Diff> libraryDiffs;
	private final Set<String> libraries;
	private final ModelReferences references;
	private final Set<DiffNode> input;
	private final TypedRefIdMap<DiffNode> selection;
	private final TypedRefIdSet visited;

	private ReferenceCheck(List<Diff> diffs, Set<DiffNode> input) {
		this.diffs = new TypedRefIdMap<>();
		this.libraries = new HashSet<>();
		this.libraryDiffs = new ArrayList<>();
		for (var diff : diffs) {
			if (!diff.isCategory && diff.type != null) {
				this.diffs.put(diff, diff);
			} else if (diff.isLibrary) {
				this.libraryDiffs.add(diff);
			}
		}
		this.input = input;
		this.selection = new TypedRefIdMap<>();
		input.stream()
				.filter(node -> node.contentAsTriDiff().type != null)
				.forEach(node -> selection.put(node.contentAsTriDiff(), node));
		this.visited = new TypedRefIdSet();
		this.references = App.exec(M.CollectingReferencesDots, () -> ModelReferences.scan(Database.get()));
	}

	static List<Diff> forRemote(List<Diff> all, Set<DiffNode> input) {
		if (!CollaborationPreference.checkReferences() || CollaborationPreference.onlyFullCommits())
			return convert(input);
		return new ReferenceCheck(all, input).run(false);
	}

	static List<Diff> forStash(List<Diff> all, Set<DiffNode> input) {
		if (!CollaborationPreference.checkReferences() || CollaborationPreference.onlyFullCommits())
			return convert(input);
		return new ReferenceCheck(all, input).run(true);
	}

	private List<Diff> run(boolean stashCommit) {
		var references = collect();
		if (references.isEmpty())
			return convert(input);
		var dialog = new CommitReferencesDialog(references, stashCommit);
		if (dialog.open() != CommitReferencesDialog.OK)
			return null;
		var selected = dialog.getSelected();
		if (selected.isEmpty())
			return convert(input);
		var set = new HashSet<DiffNode>();
		set.addAll(input);
		set.addAll(selected);
		return convert(set);
	}

	private Set<TriDiff> collect() {
		var referenced = new TypedRefIdSet();
		var stack = new Stack<TypedRefId>();
		selection.keySet().forEach(ref -> stack.add(ref));
		while (!stack.isEmpty()) {
			var next = stack.pop();
			var collected = collect(next);
			referenced.addAll(collected);
			collected.forEach(ref -> stack.add(ref));
		}
		var collected = new HashSet<TriDiff>();
		diffs.values().stream()
				.filter(referenced::contains)
				.filter(Datasets::isForeground)
				.map(diff -> new TriDiff(diff, null))
				.forEach(collected::add);
		libraryDiffs.stream()
				.filter(diff -> libraries.contains(diff.name))
				.map(diff -> new TriDiff(diff, null))
				.forEach(collected::add);
		return collected;
	}

	private TypedRefIdSet collect(TypedRefId pair) {
		var referenced = new TypedRefIdSet();
		if (visited.contains(pair))
			return referenced;
		visited.add(pair);
		references.iterateReferences(pair, ref -> {
			if (!selection.contains(ref) && diffs.contains(ref)) {
				referenced.add(ref);
			}
		});
		references.iterateUsages(pair, ref -> {
			if (!selection.contains(ref) && diffs.contains(ref) && diffs.get(ref).diffType != DiffType.ADDED) {
				referenced.add(ref);
			}
		});
		var lib = references.getLibrary(pair);
		if (lib != null) {
			libraries.add(lib);
		}
		return referenced;
	}

	private static List<Diff> convert(Set<DiffNode> nodes) {
		return nodes.stream()
				.map(node -> node.contentAsTriDiff().left)
				.collect(Collectors.toList());
	}

}
