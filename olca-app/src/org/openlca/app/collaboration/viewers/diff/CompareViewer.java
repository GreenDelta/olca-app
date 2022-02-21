package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.viewers.trees.Trees;

public class CompareViewer extends DiffNodeViewer {

	private Direction direction;
	
	public CompareViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = Trees.createViewer(parent);
		configureViewer(viewer, false);
		return viewer;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}
	
	@Override
	protected void openDiffDialog(DiffNode node) {
		var diff = node.contentAsDiffResult();
		if (diff == null)
			return;
		var left = diff.local != null ? diff.local.right : diff.remote.left;
		var right = diff.remote != null ? diff.remote.right: diff.local.left;
		DiffHelper.openDiffDialog(left, right, true, direction);
	}

}
