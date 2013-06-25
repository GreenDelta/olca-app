package org.openlca.core.application.views.navigator.filter;

import org.openlca.core.model.FlowType;

/**
 * Filter for filtering elementary flows
 */
public final class ElementaryFlowFilter extends FlowTypeFilter {

	public ElementaryFlowFilter() {
		super(FlowType.ELEMENTARY_FLOW);
	}

}
