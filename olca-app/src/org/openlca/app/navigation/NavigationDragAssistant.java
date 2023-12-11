package org.openlca.app.navigation;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.core.model.descriptors.Descriptor;

public class NavigationDragAssistant extends CommonDragAdapterAssistant {

	@Override
	public void dragStart(DragSourceEvent event, IStructuredSelection selection) {
		event.doit = true;
		for (var o : selection) {
			if (o instanceof CategoryElement catElem) {
				if (catElem.hasLibraryContent()) {
					event.doit = false;
					break;
				}
			}
		}
	}

	/**
	 * This method is only called when the drag is moved to a drop-target outside
	 * the navigator. As we only allow models to be moved outside, we only return
	 * the model-transfer here.
	 */
	@Override
	public Transfer[] getSupportedTransferTypes() {
		return new Transfer[]{ModelTransfer.getInstance()};
	}

	/**
	 * This method is called when a drop happened outside the navigation. As we
	 * only allow the drop of models outside the navigator, we only return {@code
	 * true} when the drag contains model elements.
	 */
	@Override
	public boolean setDragData(
			DragSourceEvent e, IStructuredSelection selection) {
		var descriptors = new ArrayList<Descriptor>();
		for (var o : selection) {
			if (!(o instanceof ModelElement elem))
				return false;
			if (elem.getContent() != null) {
				descriptors.add(elem.getContent());
			}
		}
		if (descriptors.isEmpty())
			return false;
		e.data = descriptors.toArray();
		return true;
	}
}
