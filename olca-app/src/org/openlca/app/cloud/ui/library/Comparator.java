package org.openlca.app.cloud.ui.library;

import java.util.Map.Entry;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.navigation.ModelTypeComparison;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

public class Comparator extends ViewerComparator {

	@Override
	@SuppressWarnings("unchecked")
	public int compare(Viewer viewer, Object o1, Object o2) {
		Entry<Dataset, String> e1 = (Entry<Dataset, String>) o1;
		Entry<Dataset, String> e2 = (Entry<Dataset, String>) o2;
		return compare(viewer, e1.getKey(), e2.getKey());
	}

	private int compare(Viewer viewer, Dataset d1, Dataset d2) {
		ModelType type1 = d1.type;
		if (type1 == ModelType.CATEGORY)
			type1 = d1.categoryType;
		ModelType type2 = d2.type;
		if (type2 == ModelType.CATEGORY)
			type2 = d2.categoryType;
		int c = ModelTypeComparison.compare(type1, type2);
		if (c != 0)
			return c;
		if (d1.type == ModelType.CATEGORY && d2.type != ModelType.CATEGORY)
			return -1;
		if (d1.type != ModelType.CATEGORY && d2.type == ModelType.CATEGORY)
			return 1;
		return Strings.compare(CloudUtil.toFullPath(d1), CloudUtil.toFullPath(d2));
	}
}
