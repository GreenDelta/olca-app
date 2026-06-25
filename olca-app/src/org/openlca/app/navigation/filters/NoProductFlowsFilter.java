package org.openlca.app.navigation.filters;

import org.openlca.core.model.FlowType;

public final class NoProductFlowsFilter extends ExcludeFlowsFilter {

	public NoProductFlowsFilter() {
		super(FlowType.PRODUCT_FLOW);
	}

}
