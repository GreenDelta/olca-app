package org.openlca.app.results.analysis.sankey;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.actions.ActionIds;

public class SankeyContextMenuProvider extends ContextMenuProvider {

	private final ActionRegistry actionRegistry;

	public SankeyContextMenuProvider(EditPartViewer viewer,
																	 ActionRegistry registry) {
		super(viewer);
		this.actionRegistry = registry;
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {
		GEFActionConstants.addStandardActionGroups(menu);

		addUndoActions(menu);
		addViewActions(menu);
		addEditActions(menu);
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

	private void addViewActions(IMenuManager menu) {
		var focus = actionRegistry.getAction(ActionIds.FOCUS);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, focus);

		var layout = actionRegistry.getAction(ActionIds.LAYOUT_TREE);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, layout);

		var openMiniature = actionRegistry.getAction(ActionIds.OPEN_MINIATURE_VIEW);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, openMiniature);
	}

	private void addEditActions(IMenuManager menu) {
		var openEditor = actionRegistry.getAction(ActionIds.OPEN_EDITOR);
		if (openEditor.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, openEditor);
	}

	private void addRestActions(IMenuManager menu) {
		var settings = actionRegistry.getAction(ActionIds.EDIT_GRAPH_CONFIG);
		menu.appendToGroup(GEFActionConstants.GROUP_REST, settings);
	}

}
