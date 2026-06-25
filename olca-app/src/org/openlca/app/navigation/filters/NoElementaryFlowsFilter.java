package org.openlca.app.navigation.filters;

import org.openlca.core.model.FlowType;

/// A filter for excluding elementary flows.
public class NoElementaryFlowsFilter extends ExcludeFlowsFilter {

	public NoElementaryFlowsFilter() {
		super(FlowType.ELEMENTARY_FLOW);
	}
}
