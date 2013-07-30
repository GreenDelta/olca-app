package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.openlca.app.dnd.ModelTransfer;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class NavigationDragAssistant extends CommonDragAdapterAssistant {

	@Override
	public void dragStart(DragSourceEvent anEvent,
			IStructuredSelection aSelection) {
		anEvent.doit = true;
		Iterator<?> it = aSelection.iterator();
		while (it.hasNext() && anEvent.doit) {
			Object o = it.next();
			if (!(o instanceof ModelElement || o instanceof CategoryElement)) {
				anEvent.doit = false;
			}
		}
	}

	@Override
	public Transfer[] getSupportedTransferTypes() {
		return new Transfer[] { ModelTransfer.getInstance() };
	}

	@Override
	public boolean setDragData(DragSourceEvent anEvent,
			IStructuredSelection aSelection) {
		boolean canBeDropped = true;
		Iterator<?> it = aSelection.iterator();
		List<BaseDescriptor> components = new ArrayList<>();
		while (it.hasNext() && canBeDropped) {
			Object o = it.next();
			if (!(o instanceof ModelElement || o instanceof CategoryElement)) {
				canBeDropped = false;
			} else {
				if (o instanceof ModelElement) {
					ModelElement navElem = (ModelElement) o;
					BaseDescriptor comp = navElem.getContent();
					if (comp != null)
						components.add(comp);
				}
			}
		}
		if (canBeDropped) {
			Object[] data = new Object[components.size()];
			for (int i = 0; i < components.size(); i++) {
				data[i] = components.get(i);
			}
			anEvent.data = data;
		}
		return canBeDropped;
	}
}
