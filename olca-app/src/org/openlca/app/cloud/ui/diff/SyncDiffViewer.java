package org.openlca.app.cloud.ui.diff;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;

class SyncDiffViewer extends DiffTreeViewer {

	public SyncDiffViewer(Composite parent, JsonLoader jsonLoader) {
		super(parent, jsonLoader);
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
