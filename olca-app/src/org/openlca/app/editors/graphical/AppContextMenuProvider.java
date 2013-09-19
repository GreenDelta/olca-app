/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
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
		menu.add(actionRegistry
				.getAction(ActionIds.BUILD_SUPPLY_CHAIN_MENU_ACTION_ID));
		menu.add(actionRegistry
				.getAction(ActionIds.REMOVE_SUPPLY_CHAIN_ACTION_ID));
		menu.add(actionRegistry
				.getAction(ActionIds.REMOVE_ALL_CONNECTIONS_ACTION_ID));
		menu.add(actionRegistry.getAction(ActionIds.EXPAND_ALL_ACTION_ID));
		menu.add(actionRegistry.getAction(ActionIds.COLLAPSE_ALL_ACTION_ID));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO,
				actionRegistry.getAction(ActionFactory.UNDO.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO,
				actionRegistry.getAction(ActionFactory.REDO.getId()));
		IAction deleteAction = actionRegistry.getAction(ActionFactory.DELETE
				.getId());
		deleteAction.setText(Messages.Delete);
		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, deleteAction);
	}
}
