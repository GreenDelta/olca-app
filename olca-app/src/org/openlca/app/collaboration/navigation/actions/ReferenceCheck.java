package org.openlca.app.collaboration.navigation.actions;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.dialogs.CommitReferenceDialog;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.core.database.IDatabase;
import org.openlca.git.model.Diff;

class ReferenceCheck {

	private final IDatabase database;

	ReferenceCheck(IDatabase database) {
		this.database = database;
	}

	List<TriDiff> run(List<TriDiff> selection, List<Diff> diffs) {
		if (!CollaborationPreference.checkReferences())
			return selection;
		var references = collect(selection, diffs);
		if (references.isEmpty())
			return selection;
		var node = new DiffNodeBuilder(database).build(references);
		var dialog = new CommitReferenceDialog(node);
		if (dialog.open() != CommitReferenceDialog.OK)
			return null;
		var selected = dialog.getSelected();
		if (selected.isEmpty())
			return selection;
		var list = new ArrayList<TriDiff>();
		list.addAll(selection);
		list.addAll(selected);
		return list;
	}

	private List<TriDiff> collect(List<TriDiff> initial, List<Diff> diffs) {
		// TODO
		return new ArrayList<>();
	}

}
