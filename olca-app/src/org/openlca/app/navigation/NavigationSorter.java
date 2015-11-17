package org.openlca.app.navigation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
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
		ModelType[] order = new ModelType[] { ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM, ModelType.IMPACT_METHOD,
				ModelType.PROCESS, ModelType.FLOW, ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP, ModelType.SOURCE, ModelType.ACTOR };
		for (int i = 0; i < order.length; i++)
			typeOrder.put(order[i], i);
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof GroupElement && e2 instanceof GroupElement)
			return 0;
		if (e1 instanceof CategoryElement && e2 instanceof ModelElement)
			return -1;
		if (e2 instanceof CategoryElement && e1 instanceof ModelElement)
			return 1;
		if (e1 instanceof ModelTypeElement && e2 instanceof ModelTypeElement)
			return compare((ModelTypeElement) e1, (ModelTypeElement) e2);
		String name1 = getLabel(viewer, e1);
		String name2 = getLabel(viewer, e2);
		return getComparator().compare(name1, name2);
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

	private String getLabel(Viewer viewer, Object e1) {
		if (viewer == null || !(viewer instanceof ContentViewer))
			return e1.toString();
		IBaseLabelProvider prov = ((ContentViewer) viewer).getLabelProvider();
		if (prov instanceof ILabelProvider) {
			ILabelProvider lprov = (ILabelProvider) prov;
			String label = lprov.getText(e1);
			if (label == null)
				return "";
			String changed = RepositoryLabel.CHANGED_STATE;
			if (label.startsWith(changed))
				return label.substring(changed.length());
			return label;
		}
		return e1.toString();
	}

}
