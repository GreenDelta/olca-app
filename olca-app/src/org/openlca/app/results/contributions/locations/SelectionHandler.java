package org.openlca.app.results.contributions.locations;

import org.openlca.app.db.Database;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.model.Currency;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
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
		page.setInput(locations.getContributions(flow), unit);
	}

	@Override
	public void impactCategorySelected(ImpactCategoryDescriptor impact) {
		if (locations == null || impact == null)
			return;
		String unit = impact.referenceUnit;
		page.setInput(locations.getContributions(impact), unit);
	}

	@Override
	public void costResultSelected(CostResultDescriptor cost) {
		if (locations == null || cost == null)
			return;
		String unit = getCurrency();
		if (cost.forAddedValue) {
			page.setInput(locations.getAddedValueContributions(), unit);
		} else {
			page.setInput(locations.getNetCostsContributions(), unit);
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