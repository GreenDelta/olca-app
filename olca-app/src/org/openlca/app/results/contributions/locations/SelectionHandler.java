package org.openlca.app.results.contributions.locations;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.LocationResult;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SelectionHandler implements Combo.EventHandler {

	private LocationPage page;
	private ContributionResult result;

	private LocationResult locations;
	private TreeContentBuilder inputBuilder;

	SelectionHandler(LocationPage page) {
		this.page = page;
		this.result = page.result;
		this.inputBuilder = new TreeContentBuilder(page);
		locations = new LocationResult(result, Database.get());
	}

	@Override
	public void flowSelected(FlowDescriptor flow) {
		if (locations == null || flow == null)
			return;
		String unit = Labels.refUnit(flow);
		List<Contribution<Location>> set = locations
				.getContributions(flow);
		double total = 0;
		for (Contribution<Location> c : set) {
			total += c.amount;
		}
		setData(set, flow, total, unit);
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		if (locations == null || impact == null)
			return;
		String unit = impact.referenceUnit;
		List<Contribution<Location>> set = locations
				.getContributions(impact);
		double total = result.getTotalImpactResult(impact);
		setData(set, impact, total, unit);
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		if (locations == null || cost == null)
			return;
		String unit = getCurrency();
		if (cost.forAddedValue) {
			List<Contribution<Location>> set = locations
					.getAddedValueContributions();
			double total = result.totalCosts;
			total = total == 0 ? 0 : -total;
			setData(set, cost, total, unit);
		} else {
			List<Contribution<Location>> set = locations
					.getNetCostsContributions();
			double total = result.totalCosts;
			setData(set, cost, total, unit);
		}
	}

	private String getCurrency() {
		try {
			CurrencyDao dao = new CurrencyDao(Database.get());
			Currency ref = dao.getReferenceCurrency();
			if (ref == null)
				return "?";
			else
				return ref.code != null ? ref.code : ref.name;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to get reference currency", e);
			return "?";
		}
	}

	private void setData(List<Contribution<Location>> set,
			Object selection, double total, String unit) {
		List<LocationItem> items = inputBuilder.build(set, selection, total);
		Collections.sort(items, (item1, item2) -> {
			if (item1.contribution == null || item2.contribution == null)
				return 0;
			Contribution<Location> c1 = item1.contribution;
			Contribution<Location> c2 = item2.contribution;
			if (Math.abs(c1.share - c2.share) > 1e-20)
				return Double.compare(c2.share, c1.share);
			else
				return Strings.compare(
						Labels.name(c1.item),
						Labels.name(c2.item));
		});
		page.setInput(items, unit);
	}
}