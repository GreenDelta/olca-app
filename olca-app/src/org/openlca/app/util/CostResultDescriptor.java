package org.openlca.app.util;

import org.openlca.app.M;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;

/**
 * This is just a utility class for re-using result components for LCC results
 * (net-costs, added values).
 */
public class CostResultDescriptor extends Descriptor {

	public boolean forAddedValue;

	private static final CostResultDescriptor addedValue;
	private static final CostResultDescriptor netCosts;
	static {
		addedValue = new CostResultDescriptor();
		addedValue.forAddedValue = true;
		addedValue.name = M.AddedValue;
		netCosts = new CostResultDescriptor();
		netCosts.forAddedValue = false;
		netCosts.name = M.NetCosts;
	}

	public CostResultDescriptor() {
		type = ModelType.CURRENCY;
	}

	public static CostResultDescriptor addedValue() {
		return addedValue;
	}

	public static CostResultDescriptor netCosts() {
		return netCosts;
	}
}
