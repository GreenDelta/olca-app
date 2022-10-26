package org.openlca.app.editors.graphical.actions;

import org.eclipse.jface.action.*;
import org.openlca.app.editors.graphical.actions.retarget.AddExchangeRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.AddProcessRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.AddStickyNoteRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.EditExchangeRetargetAction;
import org.openlca.app.tools.graphics.actions.retarget.EditConfigRetargetAction;
import org.openlca.app.tools.graphics.actions.retarget.LayoutAsTreeRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.EditStickyNoteRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.MassExpansionRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.MinMaxAllRetargetAction;
import org.openlca.app.tools.graphics.actions.retarget.OpenEditorRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.SetReferenceRetargetAction;
import org.openlca.app.tools.graphics.BasicActionBarContributor;
import org.openlca.app.tools.graphics.actions.ActionIds;

import static org.openlca.app.editors.graphical.actions.MassExpansionAction.COLLAPSE;
import static org.openlca.app.editors.graphical.actions.MassExpansionAction.EXPAND;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MINIMIZE;

public class GraphBarContributor extends BasicActionBarContributor {

	@Override
	protected void buildActions() {
		super.buildActions();
		addRetargetAction(new AddExchangeRetargetAction(true));
		addRetargetAction(new AddExchangeRetargetAction(false));
		addRetargetAction(new AddProcessRetargetAction());
		addRetargetAction(new AddStickyNoteRetargetAction());
		addRetargetAction(new EditExchangeRetargetAction());
		addRetargetAction(new EditConfigRetargetAction());
		addRetargetAction(new LayoutAsTreeRetargetAction());
		addRetargetAction(new EditStickyNoteRetargetAction());
		addRetargetAction(new MassExpansionRetargetAction(EXPAND));
		addRetargetAction(new MassExpansionRetargetAction(COLLAPSE));
		addRetargetAction(new MinMaxAllRetargetAction(MINIMIZE));
		addRetargetAction(new MinMaxAllRetargetAction(MAXIMIZE));
		addRetargetAction(new OpenEditorRetargetAction());
		addRetargetAction(new SetReferenceRetargetAction());
	}

	@Override
	public void contributeToEditMenu(IMenuManager menuManager) {
		super.contributeToEditMenu(menuManager);
		var editMenu = getEditMenu();

		var addProcess = getAction(GraphActionIds.ADD_PROCESS);
		editMenu.add(addProcess);

		var editNote = getAction(GraphActionIds.EDIT_STICKY_NOTE);
		editMenu.add(editNote);

		var addStickyNote = getAction(GraphActionIds.ADD_STICKY_NOTE);
		editMenu.add(addStickyNote);

		var openEditor = getAction(ActionIds.OPEN_EDITOR);
		editMenu.add(openEditor);
	}

	@Override
	public void contributeToViewMenu(IMenuManager menuManager) {
		super.contributeToViewMenu(menuManager);
		var viewMenu = getViewMenu();

		var layout = getActionRegistry().getAction(GraphActionIds.LAYOUT_TREE);
		viewMenu.add(layout);

		var minAll = getActionRegistry().getAction(GraphActionIds.MINIMIZE_ALL);
		viewMenu.add(minAll);

		var maxAll = getActionRegistry().getAction(GraphActionIds.MAXIMIZE_ALL);
		viewMenu.add(maxAll);

		var expandAll = getActionRegistry().getAction(GraphActionIds.EXPAND_ALL);
		viewMenu.add(expandAll);

		var collapseAll = getActionRegistry()
				.getAction(GraphActionIds.COLLAPSE_ALL);
		viewMenu.add(collapseAll);
	}

}
