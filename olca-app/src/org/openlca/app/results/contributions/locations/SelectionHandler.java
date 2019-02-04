package org.openlca.app.results.contributions.locations;

import java.util.Collections;
import java.util.List;

import org.openlca.app.components.ResultTypeSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.LocationContribution;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SelectionHandler implements EventHandler {

	private LocationPage page;
	private ContributionResult result;

	private LocationContribution calculator;
	private TreeContentBuilder inputBuilder;

	SelectionHandler(LocationPage page) {
		this.page = page;
		this.result = page.result;
		this.inputBuilder = new TreeContentBuilder(page);
		calculator = new LocationContribution(
				result, Cache.getEntityCache());
	}

	@Override
	public void flowSelected(FlowDescriptor flow) {
		if (calculator == null || flow == null)
			return;
		String unit = Labels.getRefUnit(flow);
		ContributionSet<Location> set = calculator.calculate(flow);
		double total = result.getTotalFlowResult(flow);
		setData(set, flow, total, unit);
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		if (calculator == null || impact == null)
			return;
		String unit = impact.referenceUnit;
		ContributionSet<Location> set = calculator.calculate(impact);
		double total = result.getTotalImpactResult(impact);
		setData(set, impact, total, unit);
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		if (calculator == null || cost == null)
			return;
		String unit = getCurrency();
		if (cost.forAddedValue) {
			ContributionSet<Location> set = calculator.addedValues();
			double total = result.totalCosts;
			total = total == 0 ? 0 : -total;
			setData(set, cost, total, unit);
		} else {
			ContributionSet<Location> set = calculator.netCosts();
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

	private void setData(ContributionSet<Location> set,
			BaseDescriptor selection, double total, String unit) {
		List<LocationItem> items = inputBuilder.build(set, selection, total);
		Collections.sort(items, (item1, item2) -> {
			if (item1.contribution == null || item2.contribution == null)
				return 0;
			ContributionItem<Location> c1 = item1.contribution;
			ContributionItem<Location> c2 = item2.contribution;
			if (Math.abs(c1.share - c2.share) > 1e-20)
				return Double.compare(c2.share, c1.share);
			else
				return Strings.compare(
						Labels.getDisplayName(c1.item),
						Labels.getDisplayName(c2.item));
		});
		page.setInput(items, unit);
	}
}