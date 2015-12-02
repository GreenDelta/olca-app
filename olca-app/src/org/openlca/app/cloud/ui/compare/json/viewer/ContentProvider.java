package org.openlca.app.cloud.ui.compare.json.viewer;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.openlca.app.cloud.ui.compare.json.JsonNode;

class ContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (!(inputElement instanceof Object[]))
			return null;
		Object[] array = (Object[]) inputElement;
		if (array.length == 0)
			return null;
		if (!(array[0] instanceof JsonNode))
			return null;
		return ((JsonNode) array[0]).children.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof JsonNode))
			return null;
		JsonNode node = (JsonNode) parentElement;
		return node.children.toArray();
	}

	@Override
	public Object getParent(Object element) {
		JsonNode node = (JsonNode) element;
		return node.parent;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof JsonNode))
			return false;
		JsonNode node = (JsonNode) element;
		return !node.children.isEmpty();
	}

}
