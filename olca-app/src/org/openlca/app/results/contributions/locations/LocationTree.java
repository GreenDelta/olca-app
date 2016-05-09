package org.openlca.app.results.contributions.locations;

import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
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
		viewer = new TreeViewer(parent);
		viewer.setContentProvider(new TreeContentProvider());
		treeLabel = new TreeLabel();
		viewer.setLabelProvider(treeLabel);
		Tree tree = viewer.getTree();
		GridData gridData = UI.gridData(tree, true, true);
		if (withMinHeight) {
			gridData.minimumHeight = 150;
		}
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		createColumns(tree);
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
	}

	private void createColumns(Tree tree) {
		String[] labels = { M.Location + "/" + M.Process, M.Amount, M.Unit };
		for (String col : labels) {
			TreeColumn column = new TreeColumn(tree, SWT.NONE);
			column.setText(col);
		}
		Trees.bindColumnWidths(tree, 0.4, 0.3, 0.3);
	}

	void setInput(List<LocationItem> contributions, String unit) {
		treeLabel.unit = unit;
		viewer.setInput(contributions);
	}

}
