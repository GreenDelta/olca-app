package org.openlca.app.editors.graphical;

import java.util.Collection;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.action.ActionIds;
import org.openlca.app.editors.graphical.action.AddProcessAction;
import org.openlca.app.rcp.images.Icon;

class MenuProvider extends ContextMenuProvider {

	private final ActionRegistry registry;
	private Collection<String> actionIds;

	public MenuProvider(EditPartViewer viewer, ActionRegistry registry) {
		super(viewer);
		this.registry = registry;
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {
		addEditActions(menu);
		addSupplyChainActions(menu);
		menu.add(new Separator());
		addSearchActions(menu);
		menu.add(new Separator());
		addSpecialActions(menu);
		menu.add(new Separator());
		addLayoutActions(menu);
		menu.add(new Separator());
		addActionExtensions(menu);
		menu.add(new Separator());
		addShowViewActions(menu);
	}

	public void setActionExtensions(Collection<String> actionIds) {
		this.actionIds = actionIds;
	}

	/** Undo, Redo, and Delete */
	private void addEditActions(IMenuManager menu) {
		GEFActionConstants.addStandardActionGroups(menu);
		var undo = registry.getAction(ActionFactory.UNDO.getId());
		undo.setImageDescriptor(Icon.UNDO.descriptor());
		undo.setDisabledImageDescriptor(Icon.UNDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, undo);
		
		var redo = registry.getAction(ActionFactory.REDO.getId());
		redo.setImageDescriptor(Icon.REDO.descriptor());
		redo.setDisabledImageDescriptor(Icon.REDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, redo);
		
		var delete = registry.getAction(ActionFactory.DELETE.getId());
		delete.setText(M.Delete);
		delete.setImageDescriptor(Icon.DELETE.descriptor());
		delete.setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, delete);
		
		// add process
		menu.appendToGroup(
				GEFActionConstants.GROUP_EDIT, 
				registry.getAction(AddProcessAction.ID));
	}

	private void addSupplyChainActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.BUILD_SUPPLY_CHAIN_MENU));
		menu.add(registry.getAction(ActionIds.REMOVE_SUPPLY_CHAIN));
		menu.add(registry.getAction(ActionIds.REMOVE_ALL_CONNECTIONS));
	}

	private void addSearchActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.SEARCH_PROVIDERS));
		menu.add(registry.getAction(ActionIds.SEARCH_RECIPIENTS));
	}

	private void addSpecialActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.SAVE_IMAGE));
		menu.add(registry.getAction(ActionIds.OPEN));
		menu.add(registry.getAction(ActionIds.MARK));
		menu.add(registry.getAction(ActionIds.UNMARK));
	}

	private void addLayoutActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.EXPAND_ALL));
		menu.add(registry.getAction(ActionIds.COLLAPSE_ALL));
		menu.add(registry.getAction(ActionIds.MAXIMIZE_ALL));
		menu.add(registry.getAction(ActionIds.MINIMIZE_ALL));
		menu.add(registry.getAction(ActionIds.LAYOUT_MENU));
	}

	private void addActionExtensions(IMenuManager menu) {
		if (actionIds != null)
			for (String actionId : actionIds)
				menu.add(registry.getAction(actionId));
	}

	private void addShowViewActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.SHOW_OUTLINE));
		menu.add(registry.getAction(ActionIds.OPEN_MINIATURE_VIEW));
	}
}