package org.openlca.app.results.contributions.locations;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SelectionHandler implements Combo.EventHandler {

	private LocationPage page;
	private ContributionResult result;
	private LocationResult locations;

	SelectionHandler(LocationPage page) {
		this.page = page;
		this.result = page.result;
		locations = new LocationResult(result, Database.get());
	}

	@Override
	public void flowSelected(FlowDescriptor flow) {
		if (locations == null || flow == null)
			return;
		String unit = Labels.refUnit(flow);
		List<Contribution<Location>> set = locations
				.getContributions(flow);
		page.setInput(set, unit);
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		if (locations == null || impact == null)
			return;
		String unit = impact.referenceUnit;
		List<Contribution<Location>> set = locations
				.getContributions(impact);
		page.setInput(set, unit);
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		if (locations == null || cost == null)
			return;
		String unit = getCurrency();
		if (cost.forAddedValue) {
			List<Contribution<Location>> set = locations
					.getAddedValueContributions();
			page.setInput(set, unit);
		} else {
			List<Contribution<Location>> set = locations
					.getNetCostsContributions();
			page.setInput(set, unit);
		}
	}

	private String getCurrency() {
		try {
			CurrencyDao dao = new CurrencyDao(Database.get());
			Currency ref = dao.getReferenceCurrency();
			return ref == null
					? "?"
					: ref.code != null ? ref.code : ref.name;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to get reference currency", e);
			return "?";
		}
	}

}