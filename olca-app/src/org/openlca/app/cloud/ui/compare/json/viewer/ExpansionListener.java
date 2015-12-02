package org.openlca.app.cloud.ui.compare.json.viewer;

import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.openlca.app.cloud.ui.compare.json.JsonNode;

class ExpansionListener implements ITreeViewerListener {

	private TreeViewer counterpart;

	ExpansionListener(TreeViewer counterpart) {
		this.counterpart = counterpart;
	}

	@Override
	public void treeExpanded(TreeExpansionEvent event) {
		setExpanded(event.getElement(), true);
	}

	@Override
	public void treeCollapsed(TreeExpansionEvent event) {
		setExpanded(event.getElement(), false);
	}

	private void setExpanded(Object element, boolean value) {
		if (!(element instanceof JsonNode))
			return;
		JsonNode node = (JsonNode) element;
		counterpart.setExpandedState(node, value);
	}
}
