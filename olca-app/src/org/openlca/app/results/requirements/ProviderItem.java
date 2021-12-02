package org.openlca.app.results.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.util.Labels;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.FlowType;
import org.openlca.core.results.ContributionResult;

class ProviderItem implements Item {

	final int index;
	final TechFlow product;

	double amount;
	double costValue;
	double costShare;

	private ProviderItem(int index, TechFlow product) {
		this.index = index;
		this.product = product;

	}

	static List<ProviderItem> allOf(ContributionResult result, Costs costs) {
		if (result == null || result.techIndex() == null)
			return Collections.emptyList();

		boolean withCosts = costs != null
			&& costs != Costs.NONE
			&& result.hasCosts();

		// create the items
		var items = new ArrayList<ProviderItem>();
		result.techIndex().each((index, product) -> {

			var item = new ProviderItem(index, product);
			items.add(item);
			var tr = result.totalRequirements()[index];
			item.amount = tr == 0
				? 0
				: item.hasWasteFlow() ? -tr : tr;

			// costs
			if (!withCosts)
				return;
			double c = result.provider().directCostsOf(index);
			if (c == 0)
				return;
			item.costValue = costs == Costs.NET_COSTS
				? c
				: -c;
		});

		// calculate the cost shares
		if (withCosts) {
			double maxCosts = items.stream()
				.mapToDouble(item -> item.costValue)
				.map(Math::abs)
				.max()
				.orElse(0);
			if (maxCosts > 0) {
				for (var item : items) {
					item.costShare = item.costValue / maxCosts;
				}
			}
		}

		items.sort((i1, i2) -> Double.compare(i2.amount, i1.amount));

		return items;
	}

	@Override
	public String name() {
		return Labels.of(product);
	}

	Long categoryID() {
		return product == null || product.provider() == null
			? null
			: product.provider().category;
	}

	boolean hasWasteFlow() {
		return product != null
			&& product.flow() != null
			&& product.flow().flowType == FlowType.WASTE_FLOW;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		var other = (ProviderItem) o;
		return index == other.index;
	}

	@Override
	public int hashCode() {
		return index;
	}
}
