package org.openlca.app.collaboration.viewers.json.content;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class JsonContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (!(inputElement instanceof Object[]))
			return new Object[0];
		var array = (Object[]) inputElement;
		if (array.length == 0)
			return new Object[0];
		if (!(array[0] instanceof JsonNode))
			return new Object[0];
		return ((JsonNode) array[0]).children.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof JsonNode))
			return null;
		var node = (JsonNode) parentElement;
		return node.children.toArray();
	}

	@Override
	public Object getParent(Object element) {
		var node = (JsonNode) element;
		return node.parent;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof JsonNode))
			return false;
		var node = (JsonNode) element;
		return !node.children.isEmpty();
	}

}
