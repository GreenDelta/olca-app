package org.openlca.core.editors.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.model.CostCategory;
import org.openlca.util.Doubles;

/** Data class of cost results for the display in tables and charts. */
class CostResultItem implements Comparable<CostResultItem> {

	private CostCategory costCategory;
	private double amount;
	private double contribution;
	private double total;

	public static List<CostResultItem> getItems(SimpleCostResult result) {
		if (result == null)
			return Collections.emptyList();
		CostCategory[] costCategories = result.getCostCategories();
		if (costCategories == null || costCategories.length == 0)
			return Collections.emptyList();
		double[] results = new double[costCategories.length];
		for (int i = 0; i < results.length; i++) {
			results[i] = result.getResult(costCategories[i]);
		}
		List<CostResultItem> items = makeItems(costCategories, results);
		return items;
	}

	private static List<CostResultItem> makeItems(
			CostCategory[] costCategories, double[] results) {
		double total = Doubles.sum(results);
		double min = Doubles.min(results);
		double max = Doubles.max(results);
		double ref = Math.max(Math.abs(min), Math.abs(max));
		List<CostResultItem> items = new ArrayList<>(results.length + 2);
		for (int i = 0; i < results.length; i++) {
			CostResultItem item = new CostResultItem();
			item.amount = results[i];
			if (ref != 0)
				item.contribution = results[i] / ref;
			item.costCategory = costCategories[i];
			item.total = total;
			items.add(item);
		}
		return items;
	}

	public CostCategory getCostCategory() {
		return costCategory;
	}

	public double getAmount() {
		return amount;
	}

	public double getContribution() {
		return contribution;
	}

	public double getTotal() {
		return total;
	}

	@Override
	public int compareTo(CostResultItem other) {
		return -Double.compare(this.amount, other.amount);
	}

}
