package org.openlca.app.editors.graphical;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.action.ActionIds;

class MenuProvider extends ContextMenuProvider {

	private ActionRegistry registry;

	public MenuProvider(EditPartViewer viewer, ActionRegistry actionRegistry) {
		super(viewer);
		this.registry = actionRegistry;
	}

	@Override
	public void buildContextMenu(final IMenuManager menu) {
		addEditActions(menu);
		addSupplyChainActions(menu);
		menu.add(new Separator());
		menu.add(registry.getAction(ActionIds.SAVE_IMAGE));
		menu.add(registry.getAction(ActionIds.OPEN));
		menu.add(registry.getAction(ActionIds.MARK));
		menu.add(registry.getAction(ActionIds.UNMARK));
		menu.add(new Separator());
		menu.add(registry.getAction(ActionIds.SEARCH_PROVIDERS));
		menu.add(registry.getAction(ActionIds.SEARCH_RECIPIENTS));
		menu.add(new Separator());
		addLayoutActions(menu);
		menu.add(new Separator());
		menu.add(registry.getAction(ActionIds.SHOW_OUTLINE));
		menu.add(registry.getAction(ActionIds.OPEN_MINIATURE_VIEW));
	}

	/** Undo, Redo, and Delete */
	private void addEditActions(final IMenuManager menu) {
		GEFActionConstants.addStandardActionGroups(menu);
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO,
				registry.getAction(ActionFactory.UNDO.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO,
				registry.getAction(ActionFactory.REDO.getId()));
		IAction deleteAction = registry.getAction(ActionFactory.DELETE.getId());
		deleteAction.setText(Messages.Delete);
		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, deleteAction);
	}

	private void addSupplyChainActions(final IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.BUILD_SUPPLY_CHAIN_MENU));
		menu.add(registry.getAction(ActionIds.REMOVE_SUPPLY_CHAIN));
		menu.add(registry.getAction(ActionIds.REMOVE_ALL_CONNECTIONS));
	}

	private void addLayoutActions(final IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.EXPAND_ALL));
		menu.add(registry.getAction(ActionIds.COLLAPSE_ALL));
		menu.add(registry.getAction(ActionIds.MAXIMIZE_ALL));
		menu.add(registry.getAction(ActionIds.MINIMIZE_ALL));
		menu.add(registry.getAction(ActionIds.LAYOUT_MENU));
	}
}