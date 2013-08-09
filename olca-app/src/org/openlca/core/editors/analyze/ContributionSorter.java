package org.openlca.core.editors.analyze;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.openlca.core.model.Indexable;
import org.openlca.core.results.Contribution;

/**
 * The default sorter for contributions in table viewers. The items are sorted
 * in the way that the highest contribution appears at the first position.
 */
class ContributionSorter extends ViewerSorter {

	@Override
	@SuppressWarnings("unchecked")
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (!(e1 instanceof Contribution) || !(e2 instanceof Contribution))
			return 0;
		Contribution<? extends Indexable> item1 = Contribution.class.cast(e1);
		Contribution<? extends Indexable> item2 = Contribution.class.cast(e2);
		return -Double.compare(item1.getAmount(), item2.getAmount());
	}

}
