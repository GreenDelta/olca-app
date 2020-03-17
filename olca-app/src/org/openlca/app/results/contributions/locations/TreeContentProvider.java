package org.openlca.app.results.contributions.locations;

import java.util.Collection;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.openlca.core.model.Location;
import org.openlca.core.results.Contribution;

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
	public Object[] getChildren(Object obj) {
		if (!(obj instanceof Contribution))
			return null;
		Contribution<?> c = (Contribution<?>) obj;
		if (c.childs != null && !c.childs.isEmpty())
			return c.childs.toArray();
		if (c.item instanceof Location) {
			// TODO calculate the contribution tree
			Location loc = (Location) c.item;
			System.out.println("Load contributions for " + loc);
			return new Object[0];
		}
		return null;
	}

	@Override
	public Object getParent(Object obj) {
		return null;
	}

	@Override
	public boolean hasChildren(Object obj) {
		if (!(obj instanceof Contribution))
			return false;
		Contribution<?> c = (Contribution<?>) obj;
		if (c.childs != null && !c.childs.isEmpty())
			return false;
		return c.item instanceof Location;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(
			Viewer viewer, Object old, Object newInput) {
	}

}