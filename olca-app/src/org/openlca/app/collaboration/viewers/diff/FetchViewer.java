package org.openlca.app.collaboration.viewers.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.viewers.trees.Trees;

public class FetchViewer extends DiffNodeViewer {

	private Runnable onMerge;

	public FetchViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = Trees.createViewer(parent);
		configureViewer(viewer, false);
		return viewer;
	}

	public void setOnMerge(Runnable onMerge) {
		this.onMerge = onMerge;
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
			if (!node.isModelNode())
				continue;
			DiffResult result = (DiffResult) node.content;
			if (!result.conflict())
				continue;
			if (result.overwriteLocalChanges)
				continue;
			if (result.overwriteRemoteChanges)
				continue;
			if (result.mergedData != null)
				continue;
			conflicts.add(node);
		}
		return conflicts;
	}

	@Override
	protected void openDiffDialog(DiffNode node) {
		var diff = node.contentAsDiffResult();
		if (diff == null)
			return;
		var viewMode = !diff.conflict();
		var left = diff.local != null ? diff.local.right : diff.remote.left;
		var right = diff.remote.right;
		var mergeResult = DiffHelper.openDiffDialog(left, right, viewMode, Direction.RIGHT_TO_LEFT);
		if (mergeResult == null)
			return;
		diff.reset();
		if (mergeResult.overwriteLocalChanges) {
			diff.overwriteRemoteChanges = true;
		} else {
			diff.overwriteLocalChanges = true;
		}
		diff.mergedData = mergeResult.merged;
		getViewer().refresh(node);
		if (onMerge != null) {
			onMerge.run();
		}
	}

}
