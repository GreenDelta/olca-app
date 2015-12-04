package org.openlca.app.util;

import java.util.Arrays;
import java.util.List;

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

	public static List<CostResultDescriptor> all() {
		CostResultDescriptor d1 = new CostResultDescriptor();
		d1.forAddedValue = false;
		d1.setName("#Net-costs");
		CostResultDescriptor d2 = new CostResultDescriptor();
		d2.forAddedValue = true;
		d2.setName("#Added value");
		return Arrays.asList(d1, d2);
	}

}
