package org.openlca.app.navigation.filters;

import org.openlca.core.model.FlowType;

public final class NoWasteFlowsFilter extends ExcludeFlowsFilter {

	public NoWasteFlowsFilter() {
		super(FlowType.WASTE_FLOW);
	}

}
