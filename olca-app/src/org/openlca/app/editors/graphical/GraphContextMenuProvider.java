package org.openlca.app.editors.graphical;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.BasicContextMenuProvider;

public class GraphContextMenuProvider extends BasicContextMenuProvider {

	public GraphContextMenuProvider(EditPartViewer viewer,
																	ActionRegistry registry) {
		super(viewer, registry);
	}

	@Override
	public void addEditActions(IMenuManager menu) {
		super.addEditActions(menu);

		var addProcess = getActionRegistry().getAction(GraphActionIds.ADD_PROCESS);
		if (addProcess.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, addProcess);

		var addInputExchange = getActionRegistry().getAction(
			GraphActionIds.ADD_INPUT_EXCHANGE);
		if (addInputExchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, addInputExchange);

		var addOutputExchange = getActionRegistry().getAction(
			GraphActionIds.ADD_OUTPUT_EXCHANGE);
		if (addOutputExchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, addOutputExchange);

		var setReference = getActionRegistry()
				.getAction(GraphActionIds.SET_REFERENCE);
		if (setReference.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, setReference);

		var editExchange = getActionRegistry()
				.getAction(GraphActionIds.EDIT_EXCHANGE);
		if (editExchange.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, editExchange);

		var editNote = getActionRegistry()
				.getAction(GraphActionIds.EDIT_STICKY_NOTE);
		if (editNote.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, editNote);

		var addStickyNote = getActionRegistry()
				.getAction(GraphActionIds.ADD_STICKY_NOTE);
		if (addStickyNote.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, addStickyNote);

		var delete = getActionRegistry().getAction(ActionFactory.DELETE.getId());
		delete.setImageDescriptor(Icon.DELETE.descriptor());
		delete.setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
		if (delete.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, delete);

		var removeConnections = getActionRegistry().getAction(
			GraphActionIds.REMOVE_ALL_CONNECTIONS);
		if (removeConnections.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, removeConnections);

		var buildSupplyChainMenu = getActionRegistry().
				getAction(GraphActionIds.BUILD_SUPPLY_CHAIN_MENU);
		if (buildSupplyChainMenu.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, buildSupplyChainMenu);

		var removeSupplyChain = getActionRegistry()
				.getAction(GraphActionIds.REMOVE_SUPPLY_CHAIN);
		if (removeSupplyChain.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, removeSupplyChain);

		var searchProviders = getActionRegistry()
				.getAction(GraphActionIds.SEARCH_PROVIDERS);
		if (searchProviders.isEnabled()) {
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, searchProviders);
		}

		var searchRecipients = getActionRegistry()
				.getAction(GraphActionIds.SEARCH_RECIPIENTS);
		if (searchRecipients.isEnabled()) {
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, searchRecipients);
		}

		var linkUpdate = getActionRegistry()
				.getAction(GraphActionIds.LINK_UPDATE);
		if (linkUpdate.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, linkUpdate);
	}

	public void addViewActions(IMenuManager menu) {
		super.addViewActions(menu);

		var layout = getActionRegistry().getAction(GraphActionIds.LAYOUT_TREE);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, layout);

		var min = getActionRegistry().getAction(GraphActionIds.MINIMIZE);
		if (min.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_VIEW, min);

		var max = getActionRegistry().getAction(GraphActionIds.MAXIMIZE);
		if (max.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_VIEW, max);
	}

}
