package org.openlca.app.collaboration.ui.navigation.actions;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.ui.dialogs.CommitReferenceDialog;
import org.openlca.app.collaboration.ui.preferences.CollaborationPreference;
import org.openlca.app.collaboration.ui.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.ui.viewers.diff.DiffResult;
import org.openlca.core.database.IDatabase;
import org.openlca.git.model.Diff;

class ReferenceCheck {

	private final IDatabase database;

	ReferenceCheck(IDatabase database) {
		this.database = database;
	}

	List<DiffResult> run(List<DiffResult> selection, List<Diff> diffs) {
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
		var list = new ArrayList<DiffResult>();
		list.addAll(selection);
		list.addAll(selected);
		return list;
	}

	private List<DiffResult> collect(List<DiffResult> initial, List<Diff> diffs) {
		return new ArrayList<>();
	}

}
