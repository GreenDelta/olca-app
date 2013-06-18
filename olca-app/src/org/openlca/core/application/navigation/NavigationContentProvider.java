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

import java.util.List;

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
	public Object[] getChildren(Object parent) {
		if (!(parent instanceof INavigationElement))
			return new Object[0];
		INavigationElement e = (INavigationElement) parent;
		List<INavigationElement> childs = e.getChildren();
		if (childs == null)
			return new Object[0];
		else
			return childs.toArray();
	}

	@Override
	public Object[] getElements(Object input) {
		return getChildren(input);
	}

	@Override
	public Object getParent(Object element) {
		Object object = null;
		if (element instanceof INavigationElement) {
			object = ((INavigationElement) element).getParent();
		}
		return object;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof INavigationElement))
			return false;
		INavigationElement e = (INavigationElement) element;
		return !e.getChildren().isEmpty();
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public void restoreState(IMemento aMemento) {
	}

	@Override
	public void saveState(IMemento aMemento) {
	}

}
