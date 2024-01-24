package org.openlca.app.results.slca.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.results.slca.ui.TreeModel.CategoryNode;
import org.openlca.app.results.slca.ui.TreeModel.IndicatorNode;
import org.openlca.app.results.slca.ui.TreeModel.Node;
import org.openlca.app.results.slca.ui.TreeModel.TechFlowNode;
import org.openlca.util.Strings;

public class TreeSorter extends ViewerComparator {

	@Override
	public int category(Object element) {
		if (element instanceof CategoryNode)
			return 0;
		if (element instanceof IndicatorNode)
			return 1;
		if (element instanceof TechFlowNode)
			return 2;
		return 42;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);
		if (cat1 != cat2) {
			return cat1 - cat2;
		}
		if (e1 instanceof TechFlowNode t1 && e2 instanceof TechFlowNode t2)
			return Double.compare(t2.activity(), t1.activity());
		if (e1 instanceof Node n1 && e2 instanceof Node n2)
			return Strings.compare(n1.name(), n2.name());
		return super.compare(viewer, e1, e2);
	}
}
