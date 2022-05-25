package org.openlca.app.editors.graph;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.editors.graph.actions.ActionIds;

public class GraphContextMenuProvider  extends ContextMenuProvider {

	private ActionRegistry actionRegistry;

	public GraphContextMenuProvider(EditPartViewer viewer,
																	ActionRegistry registry) {
		super(viewer);
		this.actionRegistry = registry;
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {
		GEFActionConstants.addStandardActionGroups(menu);

		IAction action;

		action = actionRegistry.getAction(ActionFactory.UNDO.getId());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, action);
		action = actionRegistry.getAction(ActionFactory.REDO.getId());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, action);

		menu.add(actionRegistry.getAction(ActionIds.LAYOUT_TREE));
	}
}
