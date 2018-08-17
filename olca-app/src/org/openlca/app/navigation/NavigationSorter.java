package org.openlca.app.navigation;

import java.util.Objects;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.openlca.core.model.ModelType;

public class NavigationSorter extends ViewerSorter {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 == null || e2 == null)
			return 0;
		if (!Objects.equals(e1.getClass(), e2.getClass()))
			return compareByType(e1, e2);
		if (e1 instanceof GroupElement && e2 instanceof GroupElement)
			return 0;
		if (e1 instanceof ModelTypeElement && e2 instanceof ModelTypeElement)
			return compare((ModelTypeElement) e1, (ModelTypeElement) e2);
		String name1 = getLabel(viewer, e1);
		String name2 = getLabel(viewer, e2);

		// TODO: document why this here makes sense: (?)
		if (e1 instanceof DatabaseElement && name1.contains(" "))
			name1 = name1.substring(0, name1.indexOf(" "));
		if (e2 instanceof DatabaseElement && name2.contains(" "))
			name2 = name2.substring(0, name2.indexOf(" "));

		return getComparator().compare(name1, name2);
	}

	private int compare(ModelTypeElement e1, ModelTypeElement e2) {
		ModelType type1 = e1.getContent();
		ModelType type2 = e2.getContent();
		return ModelTypeComparison.compare(type1, type2);
	}

	private int compareByType(Object e1, Object e2) {
		// group elements after model type elements
		if (e1 instanceof ModelTypeElement && e2 instanceof GroupElement)
			return -1;
		if (e1 instanceof GroupElement && e2 instanceof ModelTypeElement)
			return 1;
		// model elements after category elements
		if (e1 instanceof CategoryElement && e2 instanceof ModelElement)
			return -1;
		if (e2 instanceof CategoryElement && e1 instanceof ModelElement)
			return 1;
		return 0;
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
