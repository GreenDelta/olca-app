package org.openlca.app.collaboration.browse;

import java.util.Objects;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.collaboration.browse.elements.EntryElement;
import org.openlca.app.collaboration.browse.elements.GroupElement;
import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.util.Strings;

public class ServerNavigationComparator extends ViewerComparator {

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

		if (e1 instanceof EntryElement ee1 && e2 instanceof EntryElement ee2)
			return compareEntries(viewer, ee1, ee2);

		return compareLabels(viewer, e1, e2);
	}

	private int compareEntries(Viewer viewer, EntryElement e1, EntryElement e2) {
		if (e1.isModelType() && e2.isModelType())
			return ModelTypeOrder.compare(e1.getModelType(), e2.getModelType());
		if (e1.isCategory() && e2.isDataset())
			return -1;
		if (e1.isDataset() && e2.isCategory())
			return 1;
		return compareLabels(viewer, e1, e2);
	}

	private int typeOrderOf(Object o) {
		if (o instanceof EntryElement e && e.isModelType())
			return 1;
		if (o instanceof GroupElement)
			return 2;
		if (o instanceof EntryElement e && e.isCategory())
			return 3;
		if (o instanceof EntryElement e && e.isDataset())
			return 4;
		return 0;
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
			var label = provider.getText(obj);
			if (label == null)
				return "";
			return label;
		}
		return obj.toString();
	}

}
