package org.openlca.io.ui;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class SelectObjectCheckState implements ICheckStateListener {

	private CheckboxTreeViewer viewer;
	private SelectObjectsExportPage page;

	public SelectObjectCheckState(SelectObjectsExportPage page,
			CheckboxTreeViewer viewer) {
		this.page = page;
		this.viewer = viewer;
	}

	private void updateChildren(INavigationElement<?> element, boolean state) {
		for (INavigationElement<?> child : element.getChildren()) {
			// if (isVisible(child)) {
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
		boolean check = false;
		boolean all = true;
		for (INavigationElement<?> child : parent.getChildren()) {
			if (viewer.getChecked(child) || viewer.getGrayed(child)) {
				check = true;
			}
			if (!viewer.getChecked(child) || viewer.getGrayed(child)) {
				all = false;
			}
		}
		viewer.setGrayed(parent, !all && check);
		viewer.setChecked(parent, check);
		updateParent(parent);
	}

	private void updateSelection(ModelElement element, boolean selected) {
		BaseDescriptor component = element.getContent();
		if (selected)
			page.getSelectedModelComponents().add(component);
		else
			page.getSelectedModelComponents().remove(component);
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
