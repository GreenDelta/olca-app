package org.openlca.app.collaboration.browse;

import java.util.Collection;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;

public class ServerNavigationContentProvider implements ICommonContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof Collection)
			return ((Collection<?>) parent).toArray();
		if (!(parent instanceof IServerNavigationElement e))
			return new Object[0];
		var children = e.getChildren();
		return children == null
				? new Object[0]
				: children.toArray();
	}

	@Override
	public Object[] getElements(Object input) {
		return getChildren(input);
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IServerNavigationElement e)
			return e.getParent();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof IServerNavigationElement e))
			return false;
		return e.hasChildren();
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
