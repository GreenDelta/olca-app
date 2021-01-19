package org.openlca.app.navigation;

import java.util.Collection;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.openlca.app.navigation.elements.INavigationElement;

public class NavigationContentProvider implements ICommonContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof Collection)
			return ((Collection<?>) parent).toArray();
		if (!(parent instanceof INavigationElement))
			return new Object[0];
		var e = (INavigationElement<?>) parent;
		var childs = e.getChildren();
		return childs == null
				? new Object[0]
				: childs.toArray();
	}

	@Override
	public Object[] getElements(Object input) {
		return getChildren(input);
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof INavigationElement)
			return ((INavigationElement<?>) element).getParent();
		else
			return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof INavigationElement))
			return false;
		var e = (INavigationElement<?>) element;
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
