package org.openlca.app.results.contributions.locations;

import java.util.Collection;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

class TreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object obj) {
		if (obj == null)
			return new Object[0];
		if (obj instanceof Object[])
			return (Object[]) obj;
		if (obj instanceof Collection) {
			Collection<?> coll = (Collection<?>) obj;
			return coll.toArray();
		}
		return new Object[0];
	}

	@Override
	public Object[] getChildren(Object parent) {
		// TODO: we could get the location contributions here ...
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object obj) {
		return false;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object old, Object newInput) {
	}

}