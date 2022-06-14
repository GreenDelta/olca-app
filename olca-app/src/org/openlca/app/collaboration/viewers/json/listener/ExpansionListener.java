package org.openlca.app.collaboration.viewers.json.listener;

import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;

public class ExpansionListener implements ITreeViewerListener {

	private TreeViewer counterpart;

	public ExpansionListener(TreeViewer counterpart) {
		this.counterpart = counterpart;
	}

	@Override
	public void treeExpanded(TreeExpansionEvent e) {
		var source = ((TreeViewer) e.getSource()).getTree();
		setExpanded(source, e.getElement(), true);
	}

	@Override
	public void treeCollapsed(TreeExpansionEvent e) {
		var source = ((TreeViewer) e.getSource()).getTree();
		setExpanded(source, e.getElement(), false);
	}

	private void setExpanded(Tree source, Object element, boolean value) {
		counterpart.setExpandedState(element, value);
		ScrollListener.onChange(source, counterpart.getTree());
	}
}
