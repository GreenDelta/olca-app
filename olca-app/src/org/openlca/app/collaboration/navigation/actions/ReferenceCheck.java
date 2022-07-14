package org.openlca.app.collaboration.navigation.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openlca.app.App;
import org.openlca.app.collaboration.dialogs.CommitReferencesDialog;
import org.openlca.app.collaboration.navigation.actions.ModelReferences.ModelReference;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.util.TypeRefIdMap;
import org.openlca.git.util.TypedRefId;
import org.openlca.git.util.TypeRefIdSet;

class ReferenceCheck {

	private final IDatabase database;
	private final TypeRefIdMap<Diff> diffs;
	private final ModelReferences references;
	private final Set<TriDiff> input;
	private final TypeRefIdSet selection;
	private final TypeRefIdSet visited = new TypeRefIdSet();

	private ReferenceCheck(IDatabase database, List<Diff> all, Set<TriDiff> input) {
		this.database = database;
		this.diffs = TypeRefIdMap.of(all);
		this.input = input;
		this.selection = new TypeRefIdSet(input);
		this.references = App.exec("Collecting references", () -> ModelReferences.scan(Database.get()));
	}

	static Set<TriDiff> forRemote(IDatabase database, List<Diff> all, Set<TriDiff> input) {
		if (!CollaborationPreference.checkReferences())
			return input;
		return new ReferenceCheck(database, all, input).run(false);
	}

	static Set<TriDiff> forStash(IDatabase database, List<Diff> all, Set<TriDiff> input) {
		if (!CollaborationPreference.checkReferences())
			return input;
		return new ReferenceCheck(database, all, input).run(true);
	}

	private Set<TriDiff> run(boolean stashCommit) {
		var references = collect();
		if (references.isEmpty())
			return input;
		var node = new DiffNodeBuilder(database).build(references);
		var dialog = new CommitReferencesDialog(node, stashCommit);
		if (dialog.open() != CommitReferencesDialog.OK)
			return null;
		var selected = dialog.getSelected();
		if (selected.isEmpty())
			return input;
		var set = new HashSet<TriDiff>();
		set.addAll(input);
		set.addAll(selected);
		return set;
	}

	private Set<TriDiff> collect() {
		var referenced = new TypeRefIdSet();
		var stack = new Stack<TypedRefId>();
		selection.forEach(selected -> stack.add(selected));
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

}
