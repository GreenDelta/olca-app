package org.openlca.app.cloud.ui.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;

public class FetchDiffViewer extends DiffTreeViewer {

	private Runnable onMerge;

	public FetchDiffViewer(Composite parent, JsonLoader jsonLoader) {
		super(parent, jsonLoader, Direction.RIGHT_TO_LEFT);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		configureViewer(viewer, false);
		return viewer;
	}

	public void setOnMerge(Runnable onMerge) {
		this.onMerge = onMerge;
	}

	@Override
	protected void onMerge() {
		if (onMerge != null)
			onMerge.run();
	}

	@Override
	public void setInput(Collection<DiffNode> collection) {
		super.setInput(collection);
		revealConflicts();
	}

	@Override
	public void setInput(DiffNode[] input) {
		super.setInput(input);
		revealConflicts();
	}

	private void revealConflicts() {
		List<DiffNode> conflicts = getConflicts();
		for (DiffNode conflict : conflicts)
			getViewer().reveal(conflict);
	}

	public boolean hasConflicts() {
		return !getConflicts().isEmpty();
	}

	public List<DiffNode> getConflicts() {
		List<DiffNode> conflicts = new ArrayList<>();
		Stack<DiffNode> nodes = new Stack<>();
		nodes.addAll(root.children);
		while (!nodes.isEmpty()) {
			DiffNode node = nodes.pop();
			nodes.addAll(node.children);
			if (node.isModelTypeNode())
				continue;
			DiffResult result = (DiffResult) node.content;
			if (result.getType() != DiffResponse.CONFLICT)
				continue;
			if (result.overwriteLocalChanges())
				continue;
			if (result.overwriteRemoteChanges())
				continue;
			if (result.getMergedData() != null)
				continue;
			conflicts.add(node);
		}
		return conflicts;
	}
}
