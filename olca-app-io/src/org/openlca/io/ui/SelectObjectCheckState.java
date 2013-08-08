package org.openlca.io.ui;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.openlca.core.application.navigation.DataProviderNavigationElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelNavigationElement;
import org.openlca.core.model.modelprovider.IModelComponent;

public class SelectObjectCheckState implements ICheckStateListener {

	private CheckboxTreeViewer viewer;
	private SelectObjectsExportPage page;

	public SelectObjectCheckState(SelectObjectsExportPage page,
			CheckboxTreeViewer viewer) {
		this.page = page;
		this.viewer = viewer;
	}

	private void updateChildren(INavigationElement element, boolean state) {
		for (INavigationElement child : element.getChildren(false)) {
			// if (isVisible(child)) {
			viewer.setGrayed(child, false);
			viewer.setChecked(child, state);
			if (child instanceof ModelNavigationElement) {
				updateSelection((ModelNavigationElement) child,
						state);
			} else {
				updateChildren(child, state);				
			}
		}
	}

	private void updateParent(INavigationElement element) {
		if (!(element instanceof DataProviderNavigationElement)) {
			INavigationElement parent = element.getParent();
			boolean check = false;
			boolean all = true;
			for (INavigationElement child : parent.getChildren(false)) {
				// if (isVisible(child)) {
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
	}

	private void updateSelection(ModelNavigationElement element,
			boolean selected) {
		IModelComponent component = (IModelComponent) element.getData();
		if (selected) {
			page.getSelectedModelComponents().add(
					new ObjectWrapper(component, element.getDatabase()));
		} else {
			page.getSelectedModelComponents().remove(
					new ObjectWrapper(component, element.getDatabase()));
		}
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		viewer.getControl().setRedraw(false);
		INavigationElement element = (INavigationElement) event
				.getElement();
		viewer.setGrayed(element, false);
		updateChildren(element, event.getChecked());
		updateParent(element);
		if (element instanceof ModelNavigationElement) {
			updateSelection((ModelNavigationElement) element,
					event.getChecked());
		}
		viewer.getControl().setRedraw(true);
		page.checkCompletion();
	}
}
