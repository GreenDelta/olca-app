package org.openlca.app.editors.graphical;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.action.ActionIds;
import org.openlca.app.editors.graphical.action.AddFlowAction;
import org.openlca.app.editors.graphical.action.AddProcessAction;
import org.openlca.app.editors.graphical.action.EditExchangeAction;
import org.openlca.app.editors.graphical.action.GraphActions;
import org.openlca.app.editors.graphical.action.GraphSettingsAction;
import org.openlca.app.editors.graphical.action.MarkingAction;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.ModelType;

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

		var exchanges = GraphActions.allSelectedOf(editor, ExchangeNode.class);
		if (exchanges.size() == 1) {
			menu.add(new EditExchangeAction(exchanges.get(0)));
			return;
		}

		var processes = GraphActions.allSelectedOf(editor, ProcessNode.class);
		if (processes.size() == 1) {
			var node = processes.get(0);
			if (node.process != null && node.process.type == ModelType.PROCESS) {
				menu.add(AddFlowAction.forInput(node));
				menu.add(AddFlowAction.forOutput(node));
				menu.add(new Separator());
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

		menu.appendToGroup(GEFActionConstants.GROUP_EDIT,
				registry.getAction(AddProcessAction.ID));

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
	}

	private void addSearchActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.SEARCH_PROVIDERS));
		menu.add(registry.getAction(ActionIds.SEARCH_RECIPIENTS));
	}

	private void addSpecialActions(IMenuManager menu) {
		menu.add(registry.getAction(ActionIds.SAVE_IMAGE));
		menu.add(registry.getAction(ActionIds.OPEN));
		menu.add(registry.getAction(MarkingAction.MARK_ID));
		menu.add(registry.getAction(MarkingAction.UNMARK_ID));
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