/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.openlca.core.model.Category;

/**
 * Extension of the {@link ViewerSorter} to support sorting on the common viewer
 * of the applications navigator
 * 
 * @author Sebastian Greve
 * 
 */
public class NavigationSorter extends ViewerSorter {

	@Override
	public int compare(final Viewer viewer, final Object e1, final Object e2) {
		int compare = 0;
		if (e1 instanceof CategoryNavigationElement
				&& e2 instanceof ModelNavigationElement) {
			compare = -1;
		} else if (e2 instanceof CategoryNavigationElement
				&& e1 instanceof ModelNavigationElement) {
			compare = 1;
		} else if (e2 instanceof CategoryNavigationElement
				&& e1 instanceof CategoryNavigationElement
				&& ((Category) ((CategoryNavigationElement) e1).getData())
						.getId().equals(
								((Category) ((CategoryNavigationElement) e1)
										.getData()).getComponentClass())
				&& ((Category) ((CategoryNavigationElement) e2).getData())
						.getId().equals(
								((Category) ((CategoryNavigationElement) e2)
										.getData()).getComponentClass())) {
			compare = 0;
		} else {
			compare = super.compare(viewer, e1, e2);
		}
		return compare;
	}
}
