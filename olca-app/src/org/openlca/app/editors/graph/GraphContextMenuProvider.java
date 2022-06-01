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

	private final ActionRegistry actionRegistry;

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

		action = actionRegistry.getAction(ActionIds.ADD_PROCESS);
		if (action.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);

		action = actionRegistry.getAction(ActionIds.ADD_INPUT_EXCHANGE);
		if (action.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
		action = actionRegistry.getAction(ActionIds.ADD_OUTPUT_EXCHANGE);
		if (action.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);

		action = actionRegistry.getAction(ActionFactory.DELETE.getId());
		if (action.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);

		action = actionRegistry.getAction(ActionIds.LAYOUT_TREE);
		menu.appendToGroup(GEFActionConstants.GROUP_REST, action);
	}
}
