package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
			// only forbid category dragging, model elements will be checked on
			// drop in NavigationDropAssistent, to still allow e.g. dropping
			// processes in inputs/outputs table, etc.
			if (o instanceof CategoryElement catElem) {
				if (catElem.hasLibraryContent()) {
					event.doit = false;
					break;
				}
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
		List<Descriptor> components = new ArrayList<>();
		while (it.hasNext() && canBeDropped) {
			Object o = it.next();
			if (!(o instanceof ModelElement || o instanceof CategoryElement)) {
				canBeDropped = false;
			} else {
				if (o instanceof ModelElement navElem) {
					var comp = navElem.getContent();
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
