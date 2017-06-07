package org.openlca.app.cloud.ui.compare.json.viewer;

import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.ui.compare.json.JsonNode;

class ExpansionListener implements ITreeViewerListener {

	private TreeViewer counterpart;

	ExpansionListener(TreeViewer counterpart) {
		this.counterpart = counterpart;
	}

	@Override
	public void treeExpanded(TreeExpansionEvent e) {
		Tree source = ((TreeViewer) e.getSource()).getTree();
		setExpanded(source, e.getElement(), true);
	}

	@Override
	public void treeCollapsed(TreeExpansionEvent e) {
		Tree source = ((TreeViewer) e.getSource()).getTree();
		setExpanded(source, e.getElement(), false);
	}

	private void setExpanded(Tree source, Object element, boolean value) {
		if (!(element instanceof JsonNode))
			return;
		JsonNode node = (JsonNode) element;
		counterpart.setExpandedState(node, value);
		// TODO fix issue
		ScrollListener.onChange(source, counterpart.getTree());
	}
}
