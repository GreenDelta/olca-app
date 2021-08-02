package org.openlca.app.wizards.io;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;

class ModelSelectionState implements ICheckStateListener {

	private final CheckboxTreeViewer viewer;
	private final ModelSelectionPage page;

	public ModelSelectionState(
		ModelSelectionPage page, CheckboxTreeViewer viewer) {
		this.page = page;
		this.viewer = viewer;
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		viewer.getControl().setRedraw(false);
		var element = (INavigationElement<?>) event.getElement();
		viewer.setGrayed(element, false);
		updateChildren(element, event.getChecked());
		updateParent(element);
		if (element instanceof ModelElement) {
			updateSelection((ModelElement) element, event.getChecked());
		}
		viewer.getControl().setRedraw(true);
		page.checkCompletion();
	}

	void updateChildren(INavigationElement<?> element, boolean state) {
		for (var child : element.getChildren()) {
			viewer.setGrayed(child, false);
			viewer.setChecked(child, state);
			if (child instanceof ModelElement) {
				updateSelection((ModelElement) child, state);
			} else {
				updateChildren(child, state);
			}
		}
	}

	void updateParent(INavigationElement<?> element) {
		var parent = element.getParent();
		if (parent == null)
			return;

		// proof by contradiction
		boolean oneChecked = false;
		boolean allChecked = true;
		for (var child : parent.getChildren()) {
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

	void updateSelection(ModelElement element, boolean selected) {
		var model = element.getContent();
		var selection = page.getSelectedModels();
		if (model == null || selection == null)
			return;
		if (selected) {
			selection.add(model);
		} else {
			selection.remove(model);
		}
	}
}
