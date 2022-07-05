package org.openlca.app.editors.graphical;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.ActionIds;
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

		// TODO (francois) Reactivate UndoActions when every action is undoable.
		//		addUndoActions(menu);
		addEditActions(menu);
		addViewActions(menu);
		addSaveActions(menu);
		addRestActions(menu);
	}

	private void addUndoActions(IMenuManager menu) {
		var undo = actionRegistry.getAction(ActionFactory.UNDO.getId());
		undo.setImageDescriptor(Icon.UNDO.descriptor());
		undo.setDisabledImageDescriptor(Icon.UNDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, undo);

		var redo = actionRegistry.getAction(ActionFactory.REDO.getId());
		redo.setImageDescriptor(Icon.REDO.descriptor());
		redo.setDisabledImageDescriptor(Icon.REDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, redo);
	}

	private void addEditActions(IMenuManager menu) {
		var addProcess = actionRegistry.getAction(ActionIds.ADD_PROCESS);
		if (addProcess.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, addProcess);

		var addInputExchange = actionRegistry.getAction(
			ActionIds.ADD_INPUT_EXCHANGE);
		if (addInputExchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, addInputExchange);

		var addOutputExchange = actionRegistry.getAction(
			ActionIds.ADD_OUTPUT_EXCHANGE);
		if (addOutputExchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, addOutputExchange);

		var editExchange = actionRegistry.getAction(ActionIds.EDIT_EXCHANGE);
		if (editExchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, editExchange);

		var delete = actionRegistry.getAction(ActionFactory.DELETE.getId());
		delete.setText(M.Delete);
		delete.setImageDescriptor(Icon.DELETE.descriptor());
		delete.setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
		if (delete.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, delete);

		var openEditor = actionRegistry.getAction(ActionIds.OPEN_EDITOR);
		if (openEditor.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, openEditor);

		// TODO (francois) Too slow...
//		var removeConnections = actionRegistry.getAction(
//			ActionIds.REMOVE_ALL_CONNECTIONS);
//		if (removeConnections.isEnabled())
//			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, removeConnections);

		var buildSupplyChainMenu = actionRegistry.getAction(ActionIds.BUILD_SUPPLY_CHAIN_MENU);
		if (buildSupplyChainMenu.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, buildSupplyChainMenu);

		var removeSupplyChain = actionRegistry.getAction(ActionIds.REMOVE_SUPPLY_CHAIN);
		if (removeSupplyChain.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, removeSupplyChain);

		var linkUpdate = actionRegistry.getAction(ActionIds.LINK_UPDATE);
		if (linkUpdate.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, linkUpdate);

		var searchProviders = actionRegistry.getAction(ActionIds.SEARCH_PROVIDERS);
		if (searchProviders.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, searchProviders);

		var searchRecipients = actionRegistry.getAction(ActionIds.SEARCH_RECIPIENTS);
		if (searchRecipients.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, searchRecipients);
	}

	private void addViewActions(IMenuManager menu) {
		var focus = actionRegistry.getAction(ActionIds.FOCUS);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, focus);

		var layout = actionRegistry.getAction(ActionIds.LAYOUT_TREE);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, layout);

		var minAll = actionRegistry.getAction(ActionIds.MINIMIZE_ALL);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, minAll);

		var maxAll = actionRegistry.getAction(ActionIds.MAXIMIZE_ALL);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, maxAll);

		var expandAll = actionRegistry.getAction(ActionIds.EXPAND_ALL);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, expandAll);

		var collapseAll = actionRegistry.getAction(ActionIds.COLLAPSE_ALL);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, collapseAll);

		var openMiniature = actionRegistry.getAction(ActionIds.OPEN_MINIATURE_VIEW);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, openMiniature);
	}

	private void addSaveActions(IMenuManager menu) {
		var saveImage = actionRegistry.getAction(ActionIds.SAVE_IMAGE);
		menu.appendToGroup(GEFActionConstants.GROUP_SAVE, saveImage);
	}

	private void addRestActions(IMenuManager menu) {
		var settings = actionRegistry.getAction(ActionIds.EDIT_GRAPH_CONFIG);
		menu.appendToGroup(GEFActionConstants.GROUP_REST, settings);
	}

}
