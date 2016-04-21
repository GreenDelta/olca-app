package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openlca.app.util.CostResultDescriptor;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;

/**
 * Creates the content that should be displayed in the location tree for a
 * given selection.
 */
class TreeContentBuilder {

	private ContributionResultProvider<?> result;
	private Map<Location, List<ProcessDescriptor>> index = new HashMap<>();

	TreeContentBuilder(ContributionResultProvider<?> result) {
		this.result = result;
		initProcessIndex();
	}

	private void initProcessIndex() {
		if (result == null)
			return;
		EntityCache cache = result.cache;
		for (ProcessDescriptor process : result.getProcessDescriptors()) {
			Location location = null;
			if (process.getLocation() != null) {
				location = cache.get(Location.class, process.getLocation());
			}
			List<ProcessDescriptor> list = index.get(location);
			if (list == null) {
				list = new ArrayList<>();
				index.put(location, list);
			}
			list.add(process);
		}
	}

	List<LocationItem> build(ContributionSet<Location> set,
			BaseDescriptor selection, double total) {
		List<LocationItem> items = new ArrayList<>();
		for (ContributionItem<Location> contribution : set.contributions) {
			items.add(new LocationItem(contribution));
		}
		Contributions.calculate(index.keySet(), total, location -> {
			LocationItem elem = getItem(items, location);
			if (elem == null)
				return 0;
			List<ProcessDescriptor> list = index.get(location);
			double amount = 0;
			for (ProcessDescriptor p : list) {
				double r = getSingleResult(p, selection);
				ContributionItem<ProcessDescriptor> item = new ContributionItem<>();
				item.rest = p == null;
				item.item = p;
				item.amount = r;
				item.share = r / total;
				elem.processContributions.add(item);
				amount += r;
			}
			Contributions.sortDescending(elem.processContributions);
			return amount;
		});
		return items;
	}

	private double getSingleResult(ProcessDescriptor process,
			BaseDescriptor selection) {
		if (process == null || selection == null)
			return 0;
		if (selection instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor d = (ImpactCategoryDescriptor) selection;
			return result.getSingleImpactResult(process, d).value;
		}
		if (selection instanceof FlowDescriptor) {
			FlowDescriptor d = (FlowDescriptor) selection;
			return result.getSingleFlowResult(process, d).value;
		}
		if (selection instanceof CostResultDescriptor) {
			double costs = result.getSingleCostResult(process);
			CostResultDescriptor d = (CostResultDescriptor) selection;
			return d.forAddedValue ? costs == 0 ? 0 : -costs : costs;
		}
		return 0;
	}

	private LocationItem getItem(List<LocationItem> items, Location location) {
		for (LocationItem item : items) {
			Location other = item.contribution.item;
			if (Objects.equals(other, location))
				return item;
		}
		return null;
	}
}