package org.openlca.app.util;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * This is just a utility class for re-using result components for LCC results
 * (net-costs, added values).
 */
public class CostResultDescriptor extends BaseDescriptor {

	private static final long serialVersionUID = -3283456838468979294L;

	public boolean forAddedValue;

	public CostResultDescriptor() {
		setType(ModelType.CURRENCY);
	}

}
