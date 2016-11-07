package org.openlca.app.cloud.ui.diff;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.util.trees.Trees;

class SyncDiffViewer extends DiffTreeViewer {

	public SyncDiffViewer(Composite parent, JsonLoader jsonLoader) {
		super(parent, jsonLoader, true);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = Trees.createViewer(parent);
		configureViewer(viewer, false);
		return viewer;
	}

	@Override
	protected void onMerge(DiffNode node) {
		if (!node.hasChanged())
			return;
	}

}
