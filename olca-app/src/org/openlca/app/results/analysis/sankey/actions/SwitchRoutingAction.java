package org.openlca.app.results.analysis.sankey.actions;

import org.openlca.app.M;

import org.eclipse.jface.action.Action;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;

class SwitchRoutingAction extends Action {

	private final SankeyDiagram diagram;

	SwitchRoutingAction(SankeyDiagram diagram) {
		this.diagram = diagram;
		setText(M.RoutingEnabled);
		updateIcon();
	}

	@Override
	public void run() {
		diagram.switchRouting();
		updateIcon();
	}

	private void updateIcon() {
		if (diagram.isRouted()) {
			setImageDescriptor(Icon.CHECK_TRUE.descriptor());
		} else {
			setImageDescriptor(Icon.CHECK_FALSE.descriptor());
		}
	}
	
}
