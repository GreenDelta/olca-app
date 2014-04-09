package org.openlca.app.wizards.io;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.core.model.descriptors.BaseDescriptor;

class ModelSelectionState implements ICheckStateListener {

	private CheckboxTreeViewer viewer;
	private ModelSelectionPage page;

	public ModelSelectionState(ModelSelectionPage page,
			CheckboxTreeViewer viewer) {
		this.page = page;
		this.viewer = viewer;
	}

	private void updateChildren(INavigationElement<?> element, boolean state) {
		for (INavigationElement<?> child : element.getChildren()) {
			viewer.setGrayed(child, false);
			viewer.setChecked(child, state);
			if (child instanceof ModelElement) {
				updateSelection((ModelElement) child, state);
			} else {
				updateChildren(child, state);
			}
		}
	}

	private void updateParent(INavigationElement<?> element) {
		INavigationElement<?> parent = element.getParent();
		if (parent == null)
			return;
		boolean checked = false;
		boolean all = true;
		for (INavigationElement<?> child : parent.getChildren()) {
			checked = viewer.getChecked(child) || viewer.getGrayed(child);
			if (!viewer.getChecked(child) || viewer.getGrayed(child))
				all = false;
		}
		viewer.setGrayed(parent, !all && checked);
		viewer.setChecked(parent, checked);
		updateParent(parent);
	}

	private void updateSelection(ModelElement element, boolean selected) {
		BaseDescriptor component = element.getContent();
		if (selected)
			page.getSelectedModels().add(component);
		else
			page.getSelectedModels().remove(component);
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		viewer.getControl().setRedraw(false);
		INavigationElement<?> element = (INavigationElement<?>) event
				.getElement();
		viewer.setGrayed(element, false);
		updateChildren(element, event.getChecked());
		updateParent(element);
		if (element instanceof ModelElement) {
			updateSelection((ModelElement) element, event.getChecked());
		}
		viewer.getControl().setRedraw(true);
		page.checkCompletion();
	}
}
