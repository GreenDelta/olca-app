package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.jface.action.IMenuManager;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.openlca.app.tools.graphics.actions.retarget.EditConfigRetargetAction;
import org.openlca.app.tools.graphics.BasicActionBarContributor;
import org.openlca.app.tools.graphics.actions.retarget.LayoutAsTreeRetargetAction;
import org.openlca.app.tools.graphics.actions.retarget.OpenEditorRetargetAction;

public class SankeyBarContributor extends BasicActionBarContributor {

	protected void buildActions() {
		super.buildActions();
		addRetargetAction(new EditConfigRetargetAction());
		addRetargetAction(new LayoutAsTreeRetargetAction());
		addRetargetAction(new OpenEditorRetargetAction());
	}

	@Override
	public void contributeToEditMenu(IMenuManager menuManager) {
		super.contributeToEditMenu(menuManager);
		var editMenu = getEditMenu();

		var openEditor = getAction(ActionIds.OPEN_EDITOR);
		editMenu.add(openEditor);
	}

	@Override
	public void contributeToViewMenu(IMenuManager menuManager) {
		super.contributeToViewMenu(menuManager);
		var viewMenu = getViewMenu();

		var layout = getActionRegistry().getAction(GraphActionIds.LAYOUT_TREE);
		viewMenu.add(layout);
	}

}
