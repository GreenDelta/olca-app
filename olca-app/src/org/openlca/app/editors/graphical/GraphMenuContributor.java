package org.openlca.app.editors.graphical;

import static org.openlca.app.editors.graphical.actions.MassExpansionAction.*;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.*;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.openlca.app.components.graphics.BasicActionBarContributor;
import org.openlca.app.components.graphics.actions.ActionIds;
import org.openlca.app.components.graphics.actions.retarget.EditConfigRetargetAction;
import org.openlca.app.components.graphics.actions.retarget.OpenEditorRetargetAction;
import org.openlca.app.components.graphics.frame.GraphicalEditorWithFrame;
import org.openlca.app.editors.graphical.actions.retarget.AddExchangeRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.AddProcessRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.AddStickyNoteRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.EditExchangeRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.EditModeRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.EditStickyNoteRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.MassExpansionRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.MinMaxAllRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.SetReferenceRetargetAction;
import org.openlca.app.editors.graphical.actions.retarget.ShowElementaryFlowsRetargetAction;

public class GraphMenuContributor extends BasicActionBarContributor {

	public GraphMenuContributor(GraphicalEditorWithFrame editor) {
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
	public void contributeToEditMenu(IMenuManager menu) {
		super.contributeToEditMenu(menu);
		var m = getEditMenu();
		m.add(getAction(GraphActionIds.ADD_PROCESS));
		m.add(getAction(GraphActionIds.EDIT_STICKY_NOTE));
		m.add(getAction(GraphActionIds.ADD_STICKY_NOTE));
		m.add(getAction(ActionIds.OPEN_EDITOR));
	}

	@Override
	public void contributeToViewMenu(IMenuManager menu) {
		super.contributeToViewMenu(menu);
		var m = getViewMenu();
		m.add(getAction(GraphActionIds.MINIMIZE_ALL));
		m.add(getAction(GraphActionIds.MAXIMIZE_ALL));
		m.add(getAction(GraphActionIds.EXPAND_ALL));
		m.add(getAction(GraphActionIds.COLLAPSE_ALL));
	}

	public GraphConfig getConfig() {
		return getEditor() instanceof GraphEditor ge
			? ge.config
			: null;
	}

}
