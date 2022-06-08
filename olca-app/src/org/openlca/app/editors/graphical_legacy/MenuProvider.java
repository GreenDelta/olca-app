package org.openlca.app.editors.graphical_legacy;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.action.ActionIds;
import org.openlca.app.editors.graphical_legacy.action.ExchangeAddAction;
import org.openlca.app.editors.graphical_legacy.action.AddProcessAction;
import org.openlca.app.editors.graphical_legacy.action.ExchangeDeleteAction;
import org.openlca.app.editors.graphical_legacy.action.ExchangeEditAction;
import org.openlca.app.editors.graphical_legacy.action.GraphAction;
import org.openlca.app.editors.graphical_legacy.action.GraphSettingsAction;
import org.openlca.app.editors.graphical_legacy.action.LinkUpdateAction;
import org.openlca.app.editors.graphical_legacy.action.MarkingAction;
import org.openlca.app.editors.graphical_legacy.action.OpenAction;
import org.openlca.app.editors.graphical_legacy.action.SaveImageAction;
import org.openlca.app.rcp.images.Icon;

class MenuProvider extends ContextMenuProvider {

	private final GraphEditor editor;
	private final ActionRegistry registry;

	public MenuProvider(GraphEditor editor, ActionRegistry registry) {
		super(editor.getGraphicalViewer());
		this.registry = registry;
		this.editor = editor;
		editor.getGraphicalViewer().setContextMenu(this);
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {

		var actions = new GraphAction[]{
			new ExchangeEditAction(),
			new ExchangeDeleteAction(),
			new AddProcessAction(),
			ExchangeAddAction.forInput(),
			ExchangeAddAction.forOutput(),
		};
		for (var action : actions) {
			if (action.accepts(editor)) {
				menu.add(action);
			}
		}

		addEditActions(menu);
		addSupplyChainActions(menu);
		menu.add(new Separator());
		addSearchActions(menu);
		menu.add(new Separator());
		addSpecialActions(menu);
		menu.add(new Separator());
		addLayoutActions(menu);
		menu.add(new Separator());
		addShowViewActions(menu);
	}

	private void addEditActions(IMenuManager menu) {
		GEFActionConstants.addStandardActionGroups(menu);

		// undo
		var undo = registry.getAction(ActionFactory.UNDO.getId());
		undo.setImageDescriptor(Icon.UNDO.descriptor());
		undo.setDisabledImageDescriptor(Icon.UNDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, undo);

		// redo
		var redo = registry.getAction(ActionFactory.REDO.getId());
		redo.setImageDescriptor(Icon.REDO.descriptor());
		redo.setDisabledImageDescriptor(Icon.REDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, redo);

		// delete
		var delete = registry.getAction(ActionFactory.DELETE.getId());
		delete.setText(M.Delete);
		delete.setImageDescriptor(Icon.DELETE.descriptor());
		delete.setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, delete);

	}

	private void addSupplyChainActions(IMenuManager menu) {

		menu.add(registry.getAction(ActionIds.BUILD_SUPPLY_CHAIN_MENU));
		menu.add(registry.getAction(ActionIds.REMOVE_SUPPLY_CHAIN));
		menu.add(registry.getAction(ActionIds.REMOVE_ALL_CONNECTIONS));

		var linkUpdate = new LinkUpdateAction();
		if (linkUpdate.accepts(editor)) {
			menu.add(linkUpdate);
		}
	}

	private void addSearchActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.SEARCH_PROVIDERS));
		menu.add(registry.getAction(ActionIds.SEARCH_RECIPIENTS));
	}

	private void addSpecialActions(IMenuManager menu) {
		var actions = new GraphAction[]{
			new OpenAction(),
			new SaveImageAction(),
			MarkingAction.forMarking(),
			MarkingAction.forUnmarking(),
		};
		for (var action : actions) {
			if (action.accepts(editor)) {
				menu.add(action);
			}
		}
	}

	private void addLayoutActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.EXPAND_ALL));
		menu.add(registry.getAction(ActionIds.COLLAPSE_ALL));
		menu.add(registry.getAction(ActionIds.MAXIMIZE_ALL));
		menu.add(registry.getAction(ActionIds.MINIMIZE_ALL));
		menu.add(registry.getAction(ActionIds.LAYOUT_MENU));
	}

	private void addShowViewActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.SHOW_OUTLINE));
		menu.add(registry.getAction(ActionIds.OPEN_MINIATURE_VIEW));
		menu.add(new GraphSettingsAction(editor));
	}

}
