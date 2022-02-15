package org.openlca.app.collaboration.viewers.json.listener;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;

public class SelectionChangedListener implements ISelectionChangedListener {

	private TreeViewer counterpart;
	private boolean pauseListening;

	public SelectionChangedListener(TreeViewer counterpart) {
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
