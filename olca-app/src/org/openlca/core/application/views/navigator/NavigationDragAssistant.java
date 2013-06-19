/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.views.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.navigation.ModelNavigationElement;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.ui.dnd.ModelComponentTransfer;

/**
 * Extension of the {@link CommonDragAdapterAssistant} to support drag
 * assistance for the common viewer of the applications navigator
 * 
 * @author Sebastian Greve
 * 
 */
public class NavigationDragAssistant extends CommonDragAdapterAssistant {

	/**
	 * Default constructor
	 */
	public NavigationDragAssistant() {
		// nothing to initialize
	}

	@Override
	public void dragStart(final DragSourceEvent anEvent,
			final IStructuredSelection aSelection) {
		anEvent.doit = true;
		final Iterator<?> it = aSelection.iterator();

		// for each element (while no element was found which is no model
		// component or category
		while (it.hasNext() && anEvent.doit) {
			final Object o = it.next();

			// if element is not model component or category element
			if (!(o instanceof ModelNavigationElement || o instanceof CategoryElement)) {
				anEvent.doit = false;
			} else {

				// if category navigation element
				if (o instanceof CategoryElement) {
					final Category category = (Category) ((CategoryElement) o)
							.getData();
					// if component is top category
					if (category.getComponentClass().equals(category.getId())) {
						anEvent.doit = false;
					}
				}

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
			if (!(o instanceof ModelNavigationElement || o instanceof CategoryElement)) {
				canBeDropped = false;
			} else {
				// if model component element
				if (o instanceof ModelNavigationElement) {
					// cast
					final ModelNavigationElement navElem = (ModelNavigationElement) o;
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
