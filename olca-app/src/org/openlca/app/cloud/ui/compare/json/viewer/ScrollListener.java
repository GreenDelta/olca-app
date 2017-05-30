package org.openlca.app.cloud.ui.compare.json.viewer;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.openlca.app.cloud.ui.compare.json.JsonNode;

class ScrollListener extends SelectionAdapter {

	private TreeViewer counterpart;

	ScrollListener(TreeViewer otherViewer) {
		this.counterpart = otherViewer;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		Tree source = (Tree) ((ScrollBar) e.getSource()).getParent();
		onChange(source, counterpart.getTree());
	}

	static void onChange(Tree source, Tree other) {
		TreeItem itemInCounterpart = findItem(other.getItems(), source.getTopItem());
		other.setTopItem(itemInCounterpart);
	}

	private static TreeItem findItem(TreeItem[] array, TreeItem item) {
		JsonNode node = (JsonNode) item.getData();
		for (TreeItem other : array) {
			if (node == other.getData())
				return other;
			TreeItem result = findItem(other.getItems(), item);
			if (result != null)
				return result;
		}
		return null;
	}

}
