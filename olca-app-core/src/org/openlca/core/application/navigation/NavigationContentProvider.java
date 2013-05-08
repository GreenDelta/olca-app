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
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;

/**
 * Implementation of the {@link ICommonContentProvider} interface for providing
 * content for the common viewer of the applications navigator
 * 
 * @author Sebastian Greve
 * 
 */
public class NavigationContentProvider implements ICommonContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public Object[] getChildren(final Object parentElement) {
		Object[] children = new Object[0];
		if (parentElement instanceof INavigationElement) {
			children = ((INavigationElement) parentElement).getChildren(false);
		}
		return children;
	}

	@Override
	public Object[] getElements(final Object inputElement) {
		Object[] elements = new Object[0];
		if (inputElement instanceof INavigationElement) {
			elements = ((INavigationElement) inputElement).getChildren(true);
		}
		return elements;
	}

	@Override
	public Object getParent(final Object element) {
		Object object = null;
		if (element instanceof INavigationElement) {
			object = ((INavigationElement) element).getParent();
		}
		return object;
	}

	@Override
	public boolean hasChildren(final Object element) {
		boolean hasChildren = false;
		if (element instanceof INavigationElement) {
			hasChildren = ((INavigationElement) element).getChildren(true).length > 0;
		}
		return hasChildren;
	}

	@Override
	public void init(final ICommonContentExtensionSite aConfig) {

	}

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput,
			final Object newInput) {
	}

	@Override
	public void restoreState(final IMemento aMemento) {
	}

	@Override
	public void saveState(final IMemento aMemento) {
	}

}
