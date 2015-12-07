package org.openlca.app.util;

import java.util.Arrays;
import java.util.List;

import org.openlca.core.results.SimpleResultProvider;

public class CostResults {

	private CostResults() {
	}

	/**
	 * Returns the result descriptors for 'net costs' and 'added values'. The
	 * first item (=default selection) is 'net-costs' if the total cost result
	 * is positive otherwise the first item is 'added value'.
	 */
	public static List<CostResultDescriptor> getDescriptors(
			SimpleResultProvider<?> result) {
		CostResultDescriptor d1 = new CostResultDescriptor();
		d1.forAddedValue = false;
		d1.setName("#Net-costs");
		CostResultDescriptor d2 = new CostResultDescriptor();
		d2.forAddedValue = true;
		d2.setName("#Added value");
		if (result == null || result.getTotalCostResult() >= 0)
			return Arrays.asList(d1, d2);
		else
			return Arrays.asList(d2, d1);
	}

}
