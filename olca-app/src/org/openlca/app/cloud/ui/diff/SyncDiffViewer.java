package org.openlca.app.cloud.ui.diff;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;

class SyncDiffViewer extends DiffTreeViewer {

	public SyncDiffViewer(Composite parent, JsonLoader jsonLoader) {
		super(parent, jsonLoader, Direction.RIGHT_TO_LEFT);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		configureViewer(viewer, false);
		return viewer;
	}
	
	@Override
	protected void onMerge(DiffNode node) {
		if (!node.hasChanged())
			return;
	}

}
