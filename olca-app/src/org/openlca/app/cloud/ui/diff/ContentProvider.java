package org.openlca.app.cloud.ui.diff;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

class ContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		Object[] elements = (Object[]) inputElement;
		if (elements == null || elements.length == 0)
			return new Object[0];
		DiffNode node = (DiffNode) (elements)[0];
		return node.children.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		DiffNode node = (DiffNode) parentElement;
		return node.children.toArray();
	}

	@Override
	public Object getParent(Object element) {
		DiffNode node = (DiffNode) element;
		return node.parent;
	}

	@Override
	public boolean hasChildren(Object element) {
		DiffNode node = (DiffNode) element;
		return !node.children.isEmpty();
	}

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

}