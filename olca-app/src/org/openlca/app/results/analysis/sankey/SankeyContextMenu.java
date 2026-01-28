package org.openlca.app.results.analysis.sankey;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.openlca.app.components.graphics.BaseContextMenu;
import org.openlca.app.components.graphics.actions.ActionIds;

public class SankeyContextMenu extends BaseContextMenu {


	public SankeyContextMenu(EditPartViewer viewer,
																	 ActionRegistry registry) {
		super(viewer, registry);
	}

	@Override
	public void addViewActions(IMenuManager menu) {
		super.addViewActions(menu);
		var layout = getActions().getAction(ActionIds.LAYOUT_TREE);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, layout);
	}

}
