package org.openlca.app.util;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;

/**
 * This is just a utility class for re-using result components for LCC results
 * (net-costs, added values).
 */
public class CostResultDescriptor extends Descriptor {

	public boolean forAddedValue;

	public CostResultDescriptor() {
		type = ModelType.CURRENCY;
	}

}
