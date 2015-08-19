package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

class TreeContent implements ITreeContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
	}

	@Override
	public Object[] getElements(Object obj) {
		if (!(obj instanceof TreeModel))
			return null;
		TreeModel tm = (TreeModel) obj;
		return getChildren(tm.root);
	}

	@Override
	public Object[] getChildren(Object obj) {
		if (!(obj instanceof CategoryNode))
			return null;
		CategoryNode cn = (CategoryNode) obj;
		List<Object> list = new ArrayList<>();
		list.addAll(cn.childs);
		list.addAll(cn.aspects);
		return list.toArray();
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object obj) {
		return obj instanceof CategoryNode;
	}

}