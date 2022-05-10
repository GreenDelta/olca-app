package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;

abstract class DiffNodeSelectionState implements ICheckStateListener {

	private final CheckboxTreeViewer viewer;

	public DiffNodeSelectionState(CheckboxTreeViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		viewer.getControl().setRedraw(false);
		var element = (DiffNode) event.getElement();
		viewer.setGrayed(element, false);
		updateChildren(element, event.getChecked());
		updateParent(element);
		if (element.isModelNode()) {
			updateSelection(element, event.getChecked());
		}
		viewer.getControl().setRedraw(true);
		checkCompletion();
	}

	void updateChildren(DiffNode element, boolean state) {
		for (var child : element.children) {
			viewer.setGrayed(child, false);
			viewer.setChecked(child, state);
			if (child.isModelNode()) {
				updateSelection(child, state);
			} else {
				updateChildren(child, state);
			}
		}
	}

	void updateParent(DiffNode element) {
		var parent = element.parent;
		if (parent == null)
			return;

		// proof by contradiction
		boolean oneChecked = false;
		boolean allChecked = true;
		for (var child : parent.children) {
			var isChecked = viewer.getChecked(child);
			var isGrayed = viewer.getGrayed(child);
			if (isChecked || isGrayed) {
				oneChecked = true;
			}
			if (!isChecked) {
				allChecked = false;
			}
		}

		if (allChecked) {
			viewer.setChecked(parent, true);
			viewer.setGrayed(parent, false);
		} else {
			viewer.setGrayChecked(parent, oneChecked);
		}
		updateParent(parent);
	}

	protected abstract void updateSelection(DiffNode node, boolean selected);

	protected abstract void checkCompletion();
	
}
