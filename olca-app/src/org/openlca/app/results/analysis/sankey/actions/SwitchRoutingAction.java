package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.jface.action.Action;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;

class SwitchRoutingAction extends Action {

	private final SankeyDiagram diagram;
	private boolean enabled = true;

	SwitchRoutingAction(SankeyDiagram diagram) {
		this.diagram = diagram;
		setText("#Routing enabled");
		setImageDescriptor(Icon.CHECK_TRUE.descriptor());
	}

	@Override
	public void run() {
		if (enabled) {
			setImageDescriptor(Icon.CHECK_FALSE.descriptor());
			setText("#Routing enabled");
		} else {
			setText("#Routing disabled");
			setImageDescriptor(Icon.CHECK_TRUE.descriptor());
		}
		enabled = !enabled;
		diagram.node.setRouted(enabled);
	}

}
