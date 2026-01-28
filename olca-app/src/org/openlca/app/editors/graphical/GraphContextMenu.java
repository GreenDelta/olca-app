package org.openlca.app.editors.graphical;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.components.graphics.BaseContextMenu;
import org.openlca.app.components.graphics.actions.ActionIds;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;

class GraphContextMenu extends BaseContextMenu {

	public GraphContextMenu(
		EditPartViewer viewer, ActionRegistry registry) {
		super(viewer, registry);
	}

	@Override
	public void addEditActions(IMenuManager menu) {
		super.addEditActions(menu);

		onEdit(menu, GraphActionIds.ADD_PROCESS);
		onEdit(menu, GraphActionIds.SET_PROCESS_GROUP);
		onEdit(menu, GraphActionIds.ADD_INPUT_EXCHANGE);
		onEdit(menu, GraphActionIds.ADD_OUTPUT_EXCHANGE);
		onEdit(menu, GraphActionIds.SET_REFERENCE);
		onEdit(menu, GraphActionIds.EDIT_EXCHANGE);
		onEdit(menu, GraphActionIds.EDIT_STICKY_NOTE);
		onEdit(menu, GraphActionIds.ADD_STICKY_NOTE);

		var delete = getActions().getAction(ActionFactory.DELETE.getId());
		if (delete != null && delete.isEnabled()) {
			delete.setImageDescriptor(Icon.DELETE.descriptor());
			delete.setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, delete);
		}

		onEdit(menu, GraphActionIds.BUILD_SUPPLY_CHAIN_MENU);
		onEdit(menu, GraphActionIds.REMOVE_SUPPLY_CHAIN);
		onEdit(menu, GraphActionIds.SEARCH_PROVIDERS);
		onEdit(menu, GraphActionIds.SEARCH_RECIPIENTS);
		onEdit(menu, GraphActionIds.LINK_UPDATE);
	}

	private void onEdit(IMenuManager menu, String actionId) {
		var action = actions.getAction(actionId);
		if (action != null && action.isEnabled()) {
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
		}
	}

	@Override
	public void addViewActions(IMenuManager menu) {
		super.addViewActions(menu);
		onView(menu, ActionIds.LAYOUT_TREE);
		onView(menu, GraphActionIds.MINIMIZE);
		onView(menu, GraphActionIds.MAXIMIZE);
	}

	private void onView(IMenuManager menu, String actionId) {
		var action = actions.getAction(actionId);
		if (action != null && action.isEnabled()) {
			menu.appendToGroup(GEFActionConstants.GROUP_VIEW, action);
		}
	}

}
