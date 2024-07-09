package org.openlca.app.collaboration.navigation.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.CommitReferencesDialog;
import org.openlca.app.collaboration.navigation.actions.ModelReferences.ModelReference;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.util.TypedRefId;
import org.openlca.git.util.TypedRefIdMap;
import org.openlca.git.util.TypedRefIdSet;

class ReferenceCheck {

	private final IDatabase database;
	private final TypedRefIdMap<Diff> diffs;
	private final ModelReferences references;
	private final Set<DiffNode> input;
	private final TypedRefIdMap<DiffNode> selection;
	private final TypedRefIdSet visited;

	private ReferenceCheck(IDatabase database, List<Diff> all, Set<DiffNode> input) {
		this.database = database;
		this.diffs = new TypedRefIdMap<>();
		all.stream().filter(diff -> !diff.isCategory)
				.forEach(diff -> this.diffs.put(diff, diff));
		this.input = input;
		this.selection = new TypedRefIdMap<>();
		input.forEach(node -> selection.put(node.contentAsTriDiff(), node));
		this.visited = new TypedRefIdSet();
		this.references = App.exec(M.CollectingReferencesDots, () -> ModelReferences.scan(Database.get()));
	}

	static Set<Diff> forRemote(IDatabase database, List<Diff> all, Set<DiffNode> input) {
		if (!CollaborationPreference.checkReferences())
			return convert(input);
		return new ReferenceCheck(database, all, input).run(false);
	}

	static Set<Diff> forStash(IDatabase database, List<Diff> all, Set<DiffNode> input) {
		if (!CollaborationPreference.checkReferences())
			return convert(input);
		return new ReferenceCheck(database, all, input).run(true);
	}

	private Set<Diff> run(boolean stashCommit) {
		var references = collect();
		if (references.isEmpty())
			return convert(input);
		var node = new DiffNodeBuilder(database).build(references);
		var dialog = new CommitReferencesDialog(node, stashCommit);
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
		selection.forEach((type, refId, node) -> stack.add(new TypedRefId(type, refId)));
		while (!stack.isEmpty()) {
			var next = stack.pop();
			var collected = collect(next);
			referenced.addAll(collected);
			stack.addAll(collected);
		}
		return diffs.values().stream()
				.filter(referenced::contains)
				.map(diff -> new TriDiff(diff, null))
				.collect(Collectors.toSet());
	}

	private Set<ModelReference> collect(TypedRefId pair) {
		var referenced = new HashSet<ModelReference>();
		if (visited.contains(pair))
			return referenced;
		visited.add(pair);
		filter(references.getReferences(pair))
				.forEach(referenced::add);
		filter(references.getUsages(pair))
				.filter(ref -> diffs.get(ref).diffType != DiffType.ADDED)
				.forEach(referenced::add);
		return referenced;
	}

	private Stream<ModelReference> filter(Set<ModelReference> references) {
		return references.stream()
				.filter(Predicate.not(selection::contains))
				.filter(diffs::contains);
	}

	private static Set<Diff> convert(Set<DiffNode> nodes) {
		return nodes.stream()
				.map(node -> node.contentAsTriDiff().left)
				.collect(Collectors.toSet());
	}

}
