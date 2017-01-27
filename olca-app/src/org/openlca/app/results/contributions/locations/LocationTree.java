package org.openlca.app.results.contributions.locations;

import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.TreeClipboard;
import org.openlca.app.util.trees.Trees;

/**
 * Table for showing the result contributions for locations of an analysis
 * result.
 */
class LocationTree {

	private TreeViewer viewer;
	private TreeLabel treeLabel;

	public LocationTree(Composite parent, boolean withMinHeight) {
		UI.gridLayout(parent, 1);
		treeLabel = new TreeLabel();
		String[] labels = { M.Location + "/" + M.Process, M.Amount, M.Unit };
		viewer = Trees.createViewer(parent, labels, treeLabel);
		viewer.setContentProvider(new TreeContentProvider());
		Trees.bindColumnWidths(viewer.getTree(), 0.4, 0.3, 0.3);
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
		viewer.getTree().getColumns()[1].setAlignment(SWT.RIGHT);
	}

	void setInput(List<LocationItem> contributions, String unit) {
		treeLabel.unit = unit;
		viewer.setInput(contributions);
	}

}
