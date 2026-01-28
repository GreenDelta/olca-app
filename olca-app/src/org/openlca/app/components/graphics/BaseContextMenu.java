package org.openlca.app.components.graphics;

import java.util.Objects;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.components.graphics.actions.ActionIds;
import org.openlca.app.rcp.images.Icon;

public class BaseContextMenu extends ContextMenuProvider {

	protected final ActionRegistry actions;

	public BaseContextMenu(EditPartViewer viewer, ActionRegistry registry) {
		super(viewer);
		this.actions = Objects.requireNonNull(registry);
	}

	public ActionRegistry getActions() {
		return actions;
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {
		GEFActionConstants.addStandardActionGroups(menu);

//		addUndoActions(menu);
		addViewActions(menu);
		addEditActions(menu);
		addSaveActions(menu);
		addRestActions(menu);
	}

	@SuppressWarnings("unused")
	private void addUndoActions(IMenuManager menu) {
		var undo = actions.getAction(ActionFactory.UNDO.getId());
		undo.setImageDescriptor(Icon.UNDO.descriptor());
		undo.setDisabledImageDescriptor(Icon.UNDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, undo);

		var redo = actions.getAction(ActionFactory.REDO.getId());
		redo.setImageDescriptor(Icon.REDO.descriptor());
		redo.setDisabledImageDescriptor(Icon.REDO_DISABLED.descriptor());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, redo);
	}

	public void addViewActions(IMenuManager menu) {
		var focus = actions.getAction(ActionIds.FOCUS);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, focus);

		var openMiniature = actions.getAction(ActionIds.MINIMAP);
		menu.appendToGroup(GEFActionConstants.GROUP_VIEW, openMiniature);
	}

	public void addEditActions(IMenuManager menu) {
		var openEditor = actions.getAction(ActionIds.OPEN_EDITOR);
		if (openEditor.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, openEditor);
	}

	public void addSaveActions(IMenuManager menu) {
		var saveImage = getActions().getAction(ActionIds.SAVE_IMAGE);
		menu.appendToGroup(GEFActionConstants.GROUP_SAVE, saveImage);
	}

	public void addRestActions(IMenuManager menu) {
		var settings = actions.getAction(ActionIds.EDIT_CONFIG);
		menu.appendToGroup(GEFActionConstants.GROUP_REST, settings);
	}

}
