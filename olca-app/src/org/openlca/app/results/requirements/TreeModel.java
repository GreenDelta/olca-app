package org.openlca.app.results.requirements;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.openlca.core.results.ContributionResult;

class TreeModel implements ITreeContentProvider {

	private final ContributionResult result;
	private final Costs costs;

	TreeModel(ContributionResult result, Costs costs) {
		this.result = result;
		this.costs = costs;
	}

	@Override
	public Object[] getElements(Object input) {
		if (!(input instanceof ContributionResult))
			return new Object[0];
		return ProviderItem.allOf(result, costs).toArray();
	}

	@Override
	public Object[] getChildren(Object elem) {
		if (!(elem instanceof Item))
			return new Object[0];
		var item = (Item) elem;
		return item.isProvider()
			? ChildItem.allOf(item.asProvider(), result).toArray()
			: new Object[0];
	}

	@Override
	public boolean hasChildren(Object elem) {
		if (!(elem instanceof Item))
			return false;
		var item = (Item) elem;
		return item.isProvider();
	}

	@Override
	public Object getParent(Object elem) {
		return null;
	}
}
