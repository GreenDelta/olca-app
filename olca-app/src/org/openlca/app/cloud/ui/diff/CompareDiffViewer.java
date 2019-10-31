package org.openlca.app.cloud.ui.diff;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.util.trees.Trees;

class CompareDiffViewer extends DiffTreeViewer {

	CompareDiffViewer(Composite parent, JsonLoader jsonLoader) {
		// Type will be set on update, see CompareView.update
		super(parent, jsonLoader, null);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = Trees.createViewer(parent);
		configureViewer(viewer, false);
		return viewer;
	}

	@Override
	protected void onMerge(DiffNode node) {

	}

}
