package org.openlca.app.editors.graphical.actions;

import org.eclipse.jface.action.*;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.actions.retarget.AddExchangeRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.AddProcessRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.AddStickyNoteRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.EditExchangeRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.EditModeRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.ShowElementaryFlowsRetargetAction;
import org.openlca.app.tools.graphics.actions.retarget.EditConfigRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.EditStickyNoteRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.MassExpansionRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.MinMaxAllRetargetAction;
import org.openlca.app.tools.graphics.actions.retarget.OpenEditorRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.SetReferenceRetargetAction;
import org.openlca.app.tools.graphics.BasicActionBarContributor;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.openlca.app.tools.graphics.frame.GraphicalEditorWithFrame;

import static org.openlca.app.editors.graphical.actions.MassExpansionAction.COLLAPSE;
import static org.openlca.app.editors.graphical.actions.MassExpansionAction.EXPAND;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MINIMIZE;

public class GraphBarContributor extends BasicActionBarContributor {

	public GraphBarContributor(GraphicalEditorWithFrame editor) {
		super(editor);
	}

	@Override
	protected void buildActions() {
		super.buildActions();
		addRetargetAction(new AddExchangeRetargetAction(true));
		addRetargetAction(new AddExchangeRetargetAction(false));
		addRetargetAction(new AddProcessRetargetAction());
		addRetargetAction(new AddStickyNoteRetargetAction());
		addRetargetAction(new EditExchangeRetargetAction());
		addRetargetAction(new EditConfigRetargetAction());
		addRetargetAction(new EditStickyNoteRetargetAction());
		addRetargetAction(new MassExpansionRetargetAction(EXPAND));
		addRetargetAction(new MassExpansionRetargetAction(COLLAPSE));
		addRetargetAction(new MinMaxAllRetargetAction(MINIMIZE));
		addRetargetAction(new MinMaxAllRetargetAction(MAXIMIZE));
		addRetargetAction(new OpenEditorRetargetAction());
		addRetargetAction(new SetReferenceRetargetAction());
		var show = getConfig() != null && getConfig().showElementaryFlows();
		addRetargetAction(new ShowElementaryFlowsRetargetAction(show));
		var edit = getConfig() != null && getConfig().isNodeEditingEnabled();
		addRetargetAction(new EditModeRetargetAction(edit));
	}

	@Override
	public void contributeToToolBar(IToolBarManager tbm) {
		super.contributeToToolBar(tbm);
		tbm.add(getAction(GraphActionIds.SHOW_ELEMENTARY_FLOWS));
		tbm.add(getAction(GraphActionIds.EDIT_MODE));
		tbm.add(getAction(GraphActionIds.MINIMIZE_ALL));
		tbm.add(getAction(GraphActionIds.MAXIMIZE_ALL));
		tbm.add(getAction(GraphActionIds.EXPAND_ALL));
		tbm.add(getAction(GraphActionIds.COLLAPSE_ALL));
	}

	@Override
	public void contributeToEditMenu(IMenuManager menuManager) {
		super.contributeToEditMenu(menuManager);
		var editMenu = getEditMenu();

		editMenu.add(getAction(GraphActionIds.ADD_PROCESS));
		editMenu.add(getAction(GraphActionIds.EDIT_STICKY_NOTE));
		editMenu.add(getAction(GraphActionIds.ADD_STICKY_NOTE));
		editMenu.add(getAction(ActionIds.OPEN_EDITOR));
	}

	@Override
	public void contributeToViewMenu(IMenuManager menuManager) {
		super.contributeToViewMenu(menuManager);
		var viewMenu = getViewMenu();

		viewMenu.add(getAction(GraphActionIds.MINIMIZE_ALL));
		viewMenu.add(getAction(GraphActionIds.MAXIMIZE_ALL));
		viewMenu.add(getAction(GraphActionIds.EXPAND_ALL));
		viewMenu.add(getAction(GraphActionIds.COLLAPSE_ALL));
	}

	public GraphConfig getConfig() {
		if (getEditor() instanceof GraphEditor graphEditor)
			return graphEditor.config;
		else return null;
	}

}
