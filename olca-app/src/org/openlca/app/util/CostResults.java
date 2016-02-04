package org.openlca.app.util;

import org.openlca.app.M;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.SimpleResultProvider;
import org.openlca.core.results.UpstreamTree;
import org.openlca.core.results.UpstreamTreeNode;

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
		d1.setName(M.Netcosts);
		CostResultDescriptor d2 = new CostResultDescriptor();
		d2.forAddedValue = true;
		d2.setName(M.AddedValue);
		if (result == null || result.getTotalCostResult() >= 0)
			return Arrays.asList(d1, d2);
		else
			return Arrays.asList(d2, d1);
	}

	public static void forAddedValues(UpstreamTree tree) {
		if (tree == null || tree.getRoot() == null)
			return;
		Queue<UpstreamTreeNode> queue = new ArrayDeque<>();
		queue.add(tree.getRoot());
		while (!queue.isEmpty()) {
			UpstreamTreeNode node = queue.poll();
			queue.addAll(node.getChildren());
			double val = node.getAmount();
			if (val != 0)
				node.setAmount(-val);
		}
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
