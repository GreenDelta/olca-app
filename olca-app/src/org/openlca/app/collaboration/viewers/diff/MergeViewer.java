package org.openlca.app.collaboration.viewers.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.viewers.trees.Trees;

public class MergeViewer extends DiffNodeViewer {

	public MergeViewer(Composite parent) {
		super(parent, true);
		super.setDirection(Direction.RIGHT_TO_LEFT);
	}

	@Override
	public final void setDirection(Direction direction) {
		throw new UnsupportedOperationException("Can't change fetch direction");
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = Trees.createViewer(parent);
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
		List<DiffNode> conflicts = getConflicts();
		for (DiffNode conflict : conflicts) {
			getViewer().reveal(conflict);
		}
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
			TriDiff diff = (TriDiff) node.content;
			if (!diff.conflict())
				continue;
			if (getResolvedConflicts().contains(diff.type, diff.refId))
				continue;
			conflicts.add(node);
		}
		return conflicts;
	}

}