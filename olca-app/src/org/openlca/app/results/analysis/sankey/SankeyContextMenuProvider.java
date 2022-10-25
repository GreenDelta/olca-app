package org.openlca.app.results.analysis.sankey;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.results.analysis.sankey.actions.SankeyActionIds;
import org.openlca.app.tools.graphics.BasicContextMenuProvider;
import org.openlca.app.tools.graphics.actions.ActionIds;

public class SankeyContextMenuProvider extends BasicContextMenuProvider {


	public SankeyContextMenuProvider(EditPartViewer viewer,
																	 ActionRegistry registry) {
		super(viewer, registry);
	}

	@Override
	public void addViewActions(IMenuManager menu) {
		super.addViewActions(menu);
		var layout = getActionRegistry().getAction(SankeyActionIds.LAYOUT_TREE);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, layout);
	}

}
