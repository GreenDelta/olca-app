package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openlca.app.db.Cache;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;

/**
 * Creates the content that should be displayed in the location tree for a given
 * selection.
 */
class TreeContentBuilder {

	private LocationPage page;
	private ContributionResult result;
	private Map<Location, List<ProcessDescriptor>> index = new HashMap<>();

	TreeContentBuilder(LocationPage page) {
		this.page = page;
		this.result = page.result;
		initProcessIndex();
	}

	private void initProcessIndex() {
		if (result == null)
			return;
		EntityCache cache = Cache.getEntityCache();
		for (CategorizedDescriptor process : result.getProcesses()) {
			if (!(process instanceof ProcessDescriptor))
				continue;
			ProcessDescriptor p = (ProcessDescriptor) process;
			Location location = null;
			if (p.location != null) {
				location = cache.get(Location.class, p.location);
			}
			List<ProcessDescriptor> list = index.get(location);
			if (list == null) {
				list = new ArrayList<>();
				index.put(location, list);
			}
			list.add(p);
		}
	}

	List<LocationItem> build(ContributionSet<Location> set,
			BaseDescriptor selection, double total) {
		List<LocationItem> items = new ArrayList<>();
		for (ContributionItem<Location> contribution : set.contributions) {
			if (Math.abs(contribution.share) < page.cutoff)
				continue;
			if (contribution.amount == 0 && page.skipZeros)
				continue;
			items.add(new LocationItem(contribution));
		}
		Contributions.calculate(index.keySet(), total, (location) -> getAmount(location, items, selection, total));
		return items;
	}

	private double getAmount(Location location, List<LocationItem> items, BaseDescriptor selection, double total) {
		LocationItem elem = getItem(items, location);
		if (elem == null)
			return 0;
		List<ProcessDescriptor> list = index.get(location);
		double amount = 0;
		for (ProcessDescriptor p : list) {
			double r = getSingleResult(p, selection);
			if (r == 0 && page.skipZeros)
				continue;
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
	}

	private double getSingleResult(ProcessDescriptor process,
			BaseDescriptor selection) {
		if (process == null || selection == null)
			return 0;
		if (selection instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor d = (ImpactCategoryDescriptor) selection;
			return result.getDirectImpactResult(process, d);
		}
		if (selection instanceof FlowDescriptor) {
			FlowDescriptor d = (FlowDescriptor) selection;
			return result.getDirectFlowResult(process, d);
		}
		if (selection instanceof CostResultDescriptor) {
			double costs = result.getDirectCostResult(process);
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