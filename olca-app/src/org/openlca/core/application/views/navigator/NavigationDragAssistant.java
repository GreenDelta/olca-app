package org.openlca.core.application.views.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.navigation.ModelElement;
import org.openlca.core.database.IDatabase;
import org.openlca.ui.dnd.ModelComponentTransfer;

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
		return new Transfer[] { ModelComponentTransfer.getInstance() };
	}

	@Override
	public boolean setDragData(final DragSourceEvent anEvent,
			final IStructuredSelection aSelection) {
		boolean canBeDropped = true;
		final Iterator<?> it = aSelection.iterator();
		String componentClass = null;
		IDatabase database = null;
		final List<IModelComponent> components = new ArrayList<>();

		// while next and no error occured
		while (it.hasNext() && canBeDropped) {
			// next element
			final Object o = it.next();

			// if not model component or category element
			if (!(o instanceof ModelElement || o instanceof CategoryElement)) {
				canBeDropped = false;
			} else {
				// if model component element
				if (o instanceof ModelElement) {
					// cast
					final ModelElement navElem = (ModelElement) o;
					// get component
					final IModelComponent comp = (IModelComponent) navElem
							.getData();

					if (componentClass == null) {
						componentClass = comp.getClass().getCanonicalName();
						database = navElem.getDatabase();
					}

					if (database != null) {
						if (componentClass.equals(comp.getClass()
								.getCanonicalName())
								&& database.equals(navElem.getDatabase())) {
							components.add(comp);
						} else {
							canBeDropped = false;
						}
					}
				}
			}
		}
		// if can be dropped
		if (canBeDropped) {
			// set drop data
			final Object[] data = new Object[components.size() + 1];
			for (int i = 0; i < components.size(); i++) {
				data[i] = components.get(i);
			}
			data[components.size()] = database;
			anEvent.data = data;
		}
		return canBeDropped;
	}
}
