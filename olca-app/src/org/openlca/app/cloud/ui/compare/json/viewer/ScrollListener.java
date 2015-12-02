package org.openlca.app.cloud.ui.compare.json.viewer;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ScrollBar;

class ScrollListener extends SelectionAdapter {

	private TreeViewer counterpart;

	ScrollListener(TreeViewer otherViewer) {
		this.counterpart = otherViewer;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		ScrollBar bar = (ScrollBar) e.getSource();
		ScrollBar other = counterpart.getTree().getVerticalBar();
		other.setSelection(bar.getSelection());
	}

}
