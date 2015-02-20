package org.openlca.app.navigation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.openlca.core.model.ModelType;

public class NavigationSorter extends ViewerSorter {

	private Map<ModelType, Integer> typeOrder = new HashMap<>();

	public NavigationSorter() {
		super();
		fillTypeOrder();
	}

	private void fillTypeOrder() {
		ModelType[] order = new ModelType[] {
				ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM,
				ModelType.IMPACT_METHOD,
				ModelType.PROCESS,
				ModelType.FLOW,
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.SOURCE,
				ModelType.ACTOR
		};
		for (int i = 0; i < order.length; i++)
			typeOrder.put(order[i], i);
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof CategoryElement && e2 instanceof ModelElement)
			return -1;
		if (e2 instanceof CategoryElement && e1 instanceof ModelElement)
			return 1;
		if (e1 instanceof ModelTypeElement && e2 instanceof ModelTypeElement)
			return compare((ModelTypeElement) e1, (ModelTypeElement) e2);
		else
			return super.compare(viewer, e1, e2);
	}

	private int compare(ModelTypeElement e1, ModelTypeElement e2) {
		ModelType type1 = e1.getContent();
		ModelType type2 = e2.getContent();
		if (type1 == null || type2 == null)
			return 0;
		Integer order1 = typeOrder.get(type1);
		Integer order2 = typeOrder.get(type2);
		if (order1 == null || order2 == null)
			return 0;
		return order1 - order2;
	}

}
