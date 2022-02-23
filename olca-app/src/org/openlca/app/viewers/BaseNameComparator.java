package org.openlca.app.viewers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.RefEntity;
import org.openlca.core.model.descriptors.Descriptor;

/**
 * Sorts objects by their respective names.
 */
public class BaseNameComparator extends ViewerComparator {

	@Override
	public int compare(final Viewer viewer, final Object e1, final Object e2) {
		String s1 = null;
		String s2 = null;
		if (e1 instanceof RefEntity && e2 instanceof RefEntity) {
			s1 = ((RefEntity) e1).name;
			s2 = ((RefEntity) e2).name;
		} else if (e1 instanceof Exchange && e2 instanceof Exchange) {
			s1 = ((Exchange) e1).flow.name;
			s2 = ((Exchange) e2).flow.name;
		} else if (e1 instanceof Category && e2 instanceof Category) {
			s1 = ((Category) e1).name;
			s2 = ((Category) e2).name;
		} else if (e1 instanceof Parameter && e2 instanceof Parameter) {
			s1 = ((Parameter) e1).name;
			s2 = ((Parameter) e2).name;
		} else if (e1 instanceof ImpactCategory && e2 instanceof ImpactCategory) {
			s1 = ((ImpactCategory) e1).name;
			s2 = ((ImpactCategory) e2).name;
		} else if (e1 instanceof Descriptor && e2 instanceof Descriptor) {
			s1 = ((Descriptor) e1).name;
			s2 = ((Descriptor) e2).name;
		}
		return s1 != null && s2 != null ? s1.compareToIgnoreCase(s2) : 0;
	}
}
