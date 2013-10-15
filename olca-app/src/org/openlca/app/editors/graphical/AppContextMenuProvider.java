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

public class AppContextMenuProvider extends ContextMenuProvider {

	private ActionRegistry actionRegistry;

	public AppContextMenuProvider(EditPartViewer viewer,
			ActionRegistry actionRegistry) {
		super(viewer);
		this.actionRegistry = actionRegistry;
	}

	@Override
	public void buildContextMenu(final IMenuManager menu) {
		GEFActionConstants.addStandardActionGroups(menu);
		menu.add(actionRegistry.getAction(ActionIds.BUILD_SUPPLY_CHAIN_MENU));
		menu.add(actionRegistry.getAction(ActionIds.REMOVE_SUPPLY_CHAIN));
		menu.add(actionRegistry.getAction(ActionIds.REMOVE_ALL_CONNECTIONS));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO,
				actionRegistry.getAction(ActionFactory.UNDO.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO,
				actionRegistry.getAction(ActionFactory.REDO.getId()));
		IAction deleteAction = actionRegistry.getAction(ActionFactory.DELETE
				.getId());
		deleteAction.setText(Messages.Delete);
		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, deleteAction);
		menu.add(new Separator());
		menu.add(actionRegistry.getAction(ActionIds.OPEN));
		menu.add(actionRegistry.getAction(ActionIds.MARK));
		menu.add(actionRegistry.getAction(ActionIds.UNMARK));
		menu.add(new Separator());
		menu.add(actionRegistry.getAction(ActionIds.SEARCH_PROVIDERS));
		menu.add(actionRegistry.getAction(ActionIds.SEARCH_RECIPIENTS));
		menu.add(new Separator());
		menu.add(actionRegistry.getAction(ActionIds.EXPAND_ALL));
		menu.add(actionRegistry.getAction(ActionIds.COLLAPSE_ALL));
		menu.add(actionRegistry.getAction(ActionIds.MAXIMIZE_ALL));
		menu.add(actionRegistry.getAction(ActionIds.MINIMIZE_ALL));
		menu.add(actionRegistry.getAction(ActionIds.LAYOUT_MENU));
	}
}