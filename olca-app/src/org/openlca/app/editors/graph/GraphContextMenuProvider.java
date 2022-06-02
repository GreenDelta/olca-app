package org.openlca.app.editors.graph;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.graph.actions.ActionIds;
import org.openlca.app.rcp.images.Icon;

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

		var undo = actionRegistry.getAction(ActionFactory.UNDO.getId());
		undo.setImageDescriptor(Icon.UNDO.descriptor());
		undo.setDisabledImageDescriptor(Icon.UNDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, undo);

		var redo = actionRegistry.getAction(ActionFactory.REDO.getId());
		redo.setImageDescriptor(Icon.REDO.descriptor());
		redo.setDisabledImageDescriptor(Icon.REDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, redo);

		var add_process = actionRegistry.getAction(ActionIds.ADD_PROCESS);
		if (add_process.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, add_process);

		var add_input_exchange = actionRegistry.getAction(
			ActionIds.ADD_INPUT_EXCHANGE);
		if (add_input_exchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, add_input_exchange);

		var add_output_exchange = actionRegistry.getAction(
			ActionIds.ADD_OUTPUT_EXCHANGE);
		if (add_output_exchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, add_output_exchange);

		var edit_exchange = actionRegistry.getAction(ActionIds.EDIT_EXCHANGE);
		if (edit_exchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, edit_exchange);

		var delete = actionRegistry.getAction(ActionFactory.DELETE.getId());
		delete.setText(M.Delete);
		delete.setImageDescriptor(Icon.DELETE.descriptor());
		delete.setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
		if (delete.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, delete);

		var layout = actionRegistry.getAction(ActionIds.LAYOUT_TREE);
		menu.appendToGroup(GEFActionConstants.GROUP_REST, layout);

		var settings = actionRegistry.getAction(ActionIds.EDIT_GRAPH_CONFIG);
		menu.appendToGroup(GEFActionConstants.GROUP_REST, settings);
	}
}
