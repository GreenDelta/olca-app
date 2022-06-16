package org.openlca.app.collaboration.viewers.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.trees.Trees;

public class MergeViewer extends DiffNodeViewer {

	public MergeViewer(Composite parent) {
		super(parent, true);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		var viewer = Trees.createViewer(parent);
		viewer.setLabelProvider(new DiffNodeLabelProvider());
		viewer.setContentProvider(new DiffNodeContentProvider());
		viewer.setComparator(new DiffNodeComparator());
		viewer.addDoubleClickListener(this::onDoubleClick);
		return viewer;
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
		var conflicts = getConflicts();
		for (var conflict : conflicts) {
			getViewer().reveal(conflict);
		}
	}

	public boolean hasConflicts() {
		return !getConflicts().isEmpty();
	}

	public List<DiffNode> getConflicts() {
		var conflicts = new ArrayList<DiffNode>();
		var nodes = new Stack<DiffNode>();
		nodes.addAll(root.children);
		while (!nodes.isEmpty()) {
			var node = nodes.pop();
			nodes.addAll(node.children);
			if (!node.isModelNode())
				continue;
			var diff = (TriDiff) node.content;
			if (!diff.conflict())
				continue;
			if (getResolvedConflicts().contains(diff))
				continue;
			conflicts.add(node);
		}
		return conflicts;
	}

}
