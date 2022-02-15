package org.openlca.app.collaboration.viewers.json.listener;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ScrollListener extends SelectionAdapter {

	private TreeViewer counterpart;

	public ScrollListener(TreeViewer otherViewer) {
		this.counterpart = otherViewer;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		var source = (Tree) ((ScrollBar) e.getSource()).getParent();
		onChange(source, counterpart.getTree());
	}

	public static void onChange(Tree source, Tree other) {
		var itemInCounterpart = findItem(other.getItems(), source.getTopItem());
		other.setTopItem(itemInCounterpart);
	}

	private static TreeItem findItem(TreeItem[] array, TreeItem item) {
		for (var other : array) {
			if (item.getData() == other.getData())
				return other;
			var result = findItem(other.getItems(), item);
			if (result != null)
				return result;
		}
		return null;
	}

}
