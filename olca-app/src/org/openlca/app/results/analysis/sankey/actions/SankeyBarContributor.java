package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.jface.action.IMenuManager;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.openlca.app.tools.graphics.actions.retarget.EditConfigRetargetAction;
import org.openlca.app.tools.graphics.BasicActionBarContributor;
import org.openlca.app.tools.graphics.actions.retarget.OpenEditorRetargetAction;
import org.openlca.app.tools.graphics.frame.GraphicalEditorWithFrame;

public class SankeyBarContributor extends BasicActionBarContributor {

	public SankeyBarContributor(GraphicalEditorWithFrame editor) {
		super(editor);
	}

	protected void buildActions() {
		super.buildActions();
		addRetargetAction(new EditConfigRetargetAction());
		addRetargetAction(new OpenEditorRetargetAction());
	}

	@Override
	public void contributeToEditMenu(IMenuManager menuManager) {
		super.contributeToEditMenu(menuManager);
		var editMenu = getEditMenu();

		var openEditor = getAction(ActionIds.OPEN_EDITOR);
		editMenu.add(openEditor);
	}

}
