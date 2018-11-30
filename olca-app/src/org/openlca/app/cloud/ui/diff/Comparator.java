package org.openlca.app.cloud.ui.diff;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.navigation.ModelTypeComparison;
import org.openlca.core.model.ModelType;

class Comparator extends ViewerComparator {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		DiffNode node1 = (DiffNode) e1;
		DiffNode node2 = (DiffNode) e2;
		return compare(viewer, node1, node2);
	}

	private int compare(Viewer viewer, DiffNode node1, DiffNode node2) {
		if (node1.isModelTypeNode() && node2.isModelTypeNode())
			return compareModelTypes(node1, node2);
		if (node1.isCategoryNode() && node2.isModelNode())
			return -1;
		if (node1.isModelNode() && node2.isCategoryNode())
			return 1;
		return super.compare(viewer, node1, node2);
	}

	private int compareModelTypes(DiffNode node1, DiffNode node2) {
		ModelType type1 = (ModelType) node1.content;
		ModelType type2 = (ModelType) node2.content;
		return ModelTypeComparison.compare(type1, type2);
	}

}