package org.openlca.app.results.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.util.Labels;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.results.ContributionResult;

class ChildItem implements Item {

	final ProviderItem parent;
	final TechFlow product;

	double amount;
	double amountShare;

	private ChildItem(ProviderItem parent, TechFlow product) {
		this.parent = parent;
		this.product = product;
	}

	static List<ChildItem> allOf(ProviderItem parent, ContributionResult result) {
		if (parent == null || result == null)
			return Collections.emptyList();

		int row = parent.index;
		var childs = new ArrayList<ChildItem>();
		result.techIndex().each((col, product) -> {
			if (row == col)
				return;
			var amount = result.provider().scaledTechValueOf(row, col);
			if (amount == 0)
				return;
			var child = new ChildItem(parent, product);
			childs.add(child);
			child.amount = parent.hasWasteFlow()
				? amount
				: -amount;
		});

		var maxAmount = childs.stream()
			.mapToDouble(c -> c.amount)
			.map(Math::abs)
			.max()
			.orElse(0);
		for (var child : childs) {
			child.amountShare = maxAmount == 0
				? 0
				: child.amount / maxAmount;
		}

		childs.sort((i1, i2) -> Double.compare(i2.amount, i1.amount));
		return childs;
	}

	@Override
	public String name() {
		return Labels.of(product);
	}
}
