package org.openlca.app.results.requirements;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.openlca.core.results.ContributionResult;

class TreeModel implements ITreeContentProvider {

	List<ProviderItem> providers;
	List<CategoryItem> categories;

	private final ContributionResult result;
	private final Costs costs;
	private final Object[] empty = new Object[0];

	TreeModel(ContributionResult result, Costs costs) {
		this.result = result;
		this.costs = costs;
	}

	@Override
	public Object[] getElements(Object input) {
		if (!(input instanceof ContributionResult))
			return empty;
		providers = ProviderItem.allOf(result, costs);
		if (providers.size() < 20) {
			return providers.toArray();
		}
		categories = CategoryItem.allOf(providers);
		var uncat = providers.stream().filter(
			p -> p.categoryID() == null);
		return Stream.concat(categories.stream(), uncat).toArray();
	}

	@Override
	public Object[] getChildren(Object elem) {
		if (!(elem instanceof Item))
			return empty;
		var item = (Item) elem;
		if (item.isProvider())
			return ChildItem
				.allOf(item.asProvider(), result)
				.toArray();
		if (!item.isCategory() || providers == null)
			return empty;
		var catItem = item.asCategory();
		var providers = this.providers.stream()
			.filter(p -> p.categoryID() != null
				&& p.categoryID() == catItem.category.id);
		return Stream.concat(catItem.childs.stream(), providers).toArray();
	}

	@Override
	public boolean hasChildren(Object elem) {
		if (!(elem instanceof Item))
			return false;
		var item = (Item) elem;
		return item.isProvider() || item.isCategory();
	}

	@Override
	public Object getParent(Object elem) {
		if (!(elem instanceof ChildItem))
			return null;
		var child = (ChildItem) elem;
		return child.parent;
	}
}
