package org.openlca.app.results;

import org.openlca.app.util.TableColumnSorter;
import org.openlca.core.results.ContributionItem;

import com.google.common.primitives.Doubles;

/**
 * A column sorter for the share and amount fields of contribution items.
 */
@SuppressWarnings("rawtypes")
class ContributionSorter extends TableColumnSorter<ContributionItem> {

	private final boolean forShare;

	private ContributionSorter(int column, boolean forShare) {
		super(ContributionItem.class, column);
		this.forShare = forShare;
	}

	public static ContributionSorter forShare(int column) {
		return new ContributionSorter(column, true);
	}

	public static ContributionSorter forAmount(int column) {
		return new ContributionSorter(column, false);
	}

	@Override
	public int compare(ContributionItem item1, ContributionItem item2) {
		if (item1 == null || item2 == null)
			return 0;
		double val1 = forShare ? item1.getShare() : item1.getAmount();
		double val2 = forShare ? item2.getShare() : item2.getAmount();
		return Doubles.compare(val1, val2);
	}
}
