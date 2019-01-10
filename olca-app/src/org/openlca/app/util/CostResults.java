package org.openlca.app.util;

import java.util.Arrays;
import java.util.List;

import org.openlca.app.M;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.SimpleResult;

public class CostResults {

	private CostResults() {
	}

	/**
	 * Returns the result descriptors for 'net costs' and 'added values'. The first
	 * item (=default selection) is 'net-costs' if the total cost result is positive
	 * otherwise the first item is 'added value'.
	 */
	public static List<CostResultDescriptor> getDescriptors(
			SimpleResult result) {
		CostResultDescriptor d1 = new CostResultDescriptor();
		d1.forAddedValue = false;
		d1.name = M.Netcosts;
		CostResultDescriptor d2 = new CostResultDescriptor();
		d2.forAddedValue = true;
		d2.name = M.AddedValue;
		if (result == null || result.totalCosts >= 0)
			return Arrays.asList(d1, d2);
		else
			return Arrays.asList(d2, d1);
	}

	public static void forAddedValues(ContributionSet<ProcessDescriptor> set) {
		if (set == null || set.contributions == null)
			return;
		for (ContributionItem<?> item : set.contributions) {
			if (item.amount != 0)
				item.amount = -item.amount;
			if (item.share != 0)
				item.share = -item.share;
		}
	}

}
