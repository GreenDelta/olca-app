package org.openlca.app.viewers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.RefEntity;
import org.openlca.core.model.descriptors.Descriptor;

public class BaseNameComparator extends ViewerComparator {

	@Override
	public int compare(Viewer viewer, Object o1, Object o2) {
		var s1 = str(o1);
		var s2 = str(o2);
		return s1.compareToIgnoreCase(s2);
	}

	private String str(Object obj) {
		if (obj instanceof RefEntity e)
			return Labels.name(e);
		if (obj instanceof Descriptor d)
			return Labels.name(d);
		if (obj instanceof Exchange e)
			return Labels.name(e.flow);
		if (obj instanceof TechFlow tf)
			return Labels.name(tf);
		if (obj instanceof EnviFlow ef)
			return Labels.name(ef);
		return obj == null ? "" : obj.toString();
	}
}
