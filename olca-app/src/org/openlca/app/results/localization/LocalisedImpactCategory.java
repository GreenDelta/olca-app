package org.openlca.app.results.localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

/**
 * A localised impact assessment category.
 */
public class LocalisedImpactCategory {

	private ImpactCategoryDescriptor impactCategory;
	private List<LocalisedImpactFactor> factors = new ArrayList<>();

	public List<LocalisedImpactFactor> getFactors() {
		return factors;
	}

	public ImpactCategoryDescriptor getImpactCategory() {
		return impactCategory;
	}

	public void setImpactCategory(ImpactCategoryDescriptor impactCategory) {
		this.impactCategory = impactCategory;
	}

	public double getFactor(FlowDescriptor flow, Location location) {
		if (flow == null || location == null)
			return 0d;
		for (LocalisedImpactFactor factor : factors) {
			if (Objects.equals(factor.getFlow(), flow))
				return factor.getValue(location);
		}
		return 0d;
	}

}
