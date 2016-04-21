package org.openlca.app.results.contributions.locations;

import java.util.List;

import org.openlca.app.components.ResultTypeSelection.EventHandler;
import org.openlca.app.db.Database;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.LocationContribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SelectionHandler implements EventHandler {

	private LocationPage page;
	private ContributionResultProvider<?> result;

	private LocationContribution calculator;
	private TreeContentBuilder inputBuilder;

	SelectionHandler(LocationPage page) {
		this.page = page;
		this.result = page.result;
		this.inputBuilder = new TreeContentBuilder(result);
		calculator = new LocationContribution(result);
	}

	@Override
	public void flowSelected(FlowDescriptor flow) {
		if (calculator == null || flow == null)
			return;
		String unit = Labels.getRefUnit(flow, result.cache);
		ContributionSet<Location> set = calculator.calculate(flow);
		double total = result.getTotalFlowResult(flow).value;
		setData(set, flow, total, unit);
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		if (calculator == null || impact == null)
			return;
		String unit = impact.getReferenceUnit();
		ContributionSet<Location> set = calculator.calculate(impact);
		double total = result.getTotalImpactResult(impact).value;
		setData(set, impact, total, unit);
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		if (calculator == null || cost == null)
			return;
		String unit = getCurrency();
		if (cost.forAddedValue) {
			ContributionSet<Location> set = calculator.addedValues();
			double total = result.getTotalCostResult();
			total = total == 0 ? 0 : -total;
			setData(set, cost, total, unit);
		} else {
			ContributionSet<Location> set = calculator.netCosts();
			double total = result.getTotalCostResult();
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
				return ref.code != null ? ref.code : ref.getName();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to get reference currency", e);
			return "?";
		}
	}

	private void setData(ContributionSet<Location> set,
			BaseDescriptor selection, double total, String unit) {
		Contributions.sortDescending(set.contributions);
		List<LocationItem> items = inputBuilder.build(set, selection, total);
		page.setInput(items, unit);
	}
}