package org.openlca.app.cloud.ui.compare.json.viewer;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;

class SelectionChangedListener implements ISelectionChangedListener {

	private TreeViewer counterpart;
	private boolean pauseListening;

	SelectionChangedListener(TreeViewer counterpart) {
		this.counterpart = counterpart;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (pauseListening)
			return;
		pauseListening = true;
		counterpart.setSelection(event.getSelection());
		pauseListening = false;
	}

}
