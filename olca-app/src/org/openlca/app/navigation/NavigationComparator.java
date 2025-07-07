package org.openlca.app.navigation;

import java.util.Objects;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DataPackageElement;
import org.openlca.app.navigation.elements.DataPackagesElement;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.util.Strings;

public class NavigationComparator extends ViewerComparator {

	private NavigationLabelProvider labelProvider = NavigationLabelProvider.withoutRepositoryState();
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 == null || e2 == null)
			return 0;

		// for elements of different types we have a defined type order
		if (!Objects.equals(e1.getClass(), e2.getClass()))
			return typeOrderOf(e1) - typeOrderOf(e2);

		// group elements have a predefined order
		if (e1 instanceof GroupElement)
			return 0;

		if (e1 instanceof ModelTypeElement m1 && e2 instanceof ModelTypeElement m2)
			return ModelTypeOrder.compare(m1.getContent(), m2.getContent());

		// for script elements folders come before files
		if (e1 instanceof ScriptElement s1 && e2 instanceof ScriptElement s2) {
			var f1 = s1.getContent();
			var f2 = s2.getContent();
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;
			if (!f1.isDirectory() && f2.isDirectory())
				return 1;
			return Strings.compare(f1.getName(), f2.getName());
		}

		return compareLabels(viewer, e1, e2);
	}

	private int typeOrderOf(Object o) {
		if (o instanceof DatabaseDirElement)
			return 0;
		if (o instanceof DatabaseElement)
			return 1;
		if (o instanceof ModelTypeElement)
			return 2;
		if (o instanceof GroupElement)
			return 3;
		if (o instanceof CategoryElement)
			return 4;
		if (o instanceof ModelElement)
			return 5;
		if (o instanceof DataPackagesElement)
			return 6;
		if (o instanceof DataPackageElement)
			return 7;
		if (o instanceof ScriptElement)
			return 8;
		return 9;
	}

	private int compareLabels(Viewer viewer, Object e1, Object e2) {
		String name1 = getLabel(viewer, e1);
		String name2 = getLabel(viewer, e2);
		return Strings.compare(name1, name2);
	}

	private String getLabel(Viewer viewer, Object obj) {
		if (obj == null)
			return "";
		if (!(viewer instanceof ContentViewer cv))
			return obj.toString();
		var prov = cv.getLabelProvider();
		if (prov instanceof ILabelProvider provider) {
			var label = "";
			if (obj instanceof INavigationElement elem) {
				// avoid different sorting depending on the repository state
				label = labelProvider.getBaseText(elem);
			} else {
				label = provider.getText(obj);
			}
			if (label == null)
				return "";
			return label;
		}
		return obj.toString();
	}

	
}
