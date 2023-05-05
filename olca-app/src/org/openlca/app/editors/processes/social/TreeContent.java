package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;

class TreeContent implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object obj) {
		if (!(obj instanceof TreeModel tm))
			return null;
		return getChildren(tm.root);
	}

	@Override
	public Object[] getChildren(Object obj) {
		if (!(obj instanceof CategoryNode cn))
			return null;
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
