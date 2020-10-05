package org.openlca.app.cloud.ui.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.viewers.trees.Trees;

public class FetchDiffViewer extends DiffTreeViewer {

	private Runnable onMerge;

	public FetchDiffViewer(Composite parent, JsonLoader jsonLoader) {
		super(parent, jsonLoader, ActionType.FETCH);
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
	protected void onMerge(DiffNode node) {
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

}
