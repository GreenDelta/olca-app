package org.openlca.app.cloud.ui.library;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.cloud.model.LibraryRestriction;
import org.openlca.cloud.model.RestrictionType;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

public class Comparator extends ViewerComparator {

	@Override
	public int compare(Viewer viewer, Object o1, Object o2) {
		LibraryRestriction l1 = (LibraryRestriction) o1;
		LibraryRestriction l2 = (LibraryRestriction) o2;
		if (l1.type == RestrictionType.FORBIDDEN && l2.type == RestrictionType.WARNING)
			return -1;
		if (l1.type == RestrictionType.WARNING && l2.type == RestrictionType.FORBIDDEN)
			return 1;
		return compare(viewer, l1.dataset, l2.dataset);
	}

	private int compare(Viewer viewer, Dataset d1, Dataset d2) {
		ModelType type1 = d1.type == ModelType.CATEGORY ? d1.categoryType : d1.type;
		ModelType type2 = d2.type == ModelType.CATEGORY ? d2.categoryType : d2.type;
		int c = ModelTypeOrder.compare(type1, type2);
		if (c != 0)
			return c;
		if (d1.type == ModelType.CATEGORY && d2.type != ModelType.CATEGORY)
			return -1;
		if (d1.type != ModelType.CATEGORY && d2.type == ModelType.CATEGORY)
			return 1;
		return Strings.compare(CloudUtil.toFullPath(d1), CloudUtil.toFullPath(d2));
	}
}
