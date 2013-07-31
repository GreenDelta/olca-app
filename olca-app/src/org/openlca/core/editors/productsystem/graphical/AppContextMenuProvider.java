/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.Messages;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.editors.productsystem.graphical.actions.BuildSupplyChainMenuAction;
import org.openlca.core.editors.productsystem.graphical.actions.GetLinksPopupAction;
import org.openlca.core.editors.productsystem.graphical.actions.MarkProcessAction;
import org.openlca.core.editors.productsystem.graphical.actions.RemoveConnectionsFromProcessAction;
import org.openlca.core.editors.productsystem.graphical.actions.RemoveSupplyChainAction;

/**
 * The Context menu provider of this graphical editor
 * 
 * @see ContextMenuProvider
 * 
 * @author Sebastian Greve
 * 
 */
public class AppContextMenuProvider extends ContextMenuProvider {

	/**
	 * The action registry
	 */
	private ActionRegistry actionRegistry;

	/**
	 * Creates a new context menu provider
	 * 
	 * @param viewer
	 *            The edit part viewer of the graphical editor
	 * @param registry
	 *            The action registry
	 */
	public AppContextMenuProvider(final EditPartViewer viewer,
			final ActionRegistry registry) {
		super(viewer);
		setActionRegistry(registry);
	}

	/**
	 * Getter of the actionRegistry-field
	 * 
	 * @return The action registry
	 */
	private ActionRegistry getActionRegistry() {
		return actionRegistry;
	}

	/**
	 * Setter of the actionRegistry-field
	 * 
	 * @param registry
	 *            The new action registry
	 */
	private void setActionRegistry(final ActionRegistry registry) {
		actionRegistry = registry;
	}

	@Override
	public void buildContextMenu(final IMenuManager menu) {
		GEFActionConstants.addStandardActionGroups(menu);
		menu.add(getActionRegistry().getAction(OpenEditorAction.ID));
		menu.add(new Separator());
		menu.add(getActionRegistry().getAction(MarkProcessAction.ID + true));
		menu.add(getActionRegistry().getAction(MarkProcessAction.ID + false));
		menu.add(new Separator());
		menu.add(getActionRegistry().getAction(GetLinksPopupAction.ID + true));
		menu.add(getActionRegistry().getAction(GetLinksPopupAction.ID + false));
		menu.add(new Separator());
		menu.add(getActionRegistry().getAction(BuildSupplyChainMenuAction.ID));
		menu.add(getActionRegistry().getAction(RemoveSupplyChainAction.ID));
		menu.add(getActionRegistry().getAction(
				RemoveConnectionsFromProcessAction.ID));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, getActionRegistry()
				.getAction(ActionFactory.UNDO.getId()));
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, getActionRegistry()
				.getAction(ActionFactory.REDO.getId()));
		final IAction deleteAction = getActionRegistry().getAction(
				ActionFactory.DELETE.getId());
		deleteAction.setText(Messages.Common_Delete);
		menu.appendToGroup(GEFActionConstants.GROUP_EDIT, deleteAction);
	}

}
