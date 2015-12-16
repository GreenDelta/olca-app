package org.openlca.app.cloud.ui.diff;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;

public class ReferencesDiffViewer extends DiffTreeViewer {

	public ReferencesDiffViewer(Composite parent, JsonLoader jsonLoader) {
		super(parent, jsonLoader, Direction.LEFT_TO_RIGHT);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		configureViewer(viewer, false);
		return viewer;
	}

}
