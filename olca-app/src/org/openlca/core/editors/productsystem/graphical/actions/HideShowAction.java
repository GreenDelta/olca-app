/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.actions;

import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openlca.app.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.editors.productsystem.graphical.outline.ProcessTreeEditPart;

/**
 * Action for showing or hiding a process node
 * 
 * @author Sebastian Greve
 * 
 */
public class HideShowAction extends Action {

	/**
	 * The product system node of the graphical viewer
	 */
	private ProductSystemNode productSystemNode;

	/**
	 * Indicates if this action should show or hide a specific process
	 */
	private final boolean show;

	/**
	 * The outline tree viewer
	 */
	private TreeViewer viewer;

	/**
	 * Creates a new hide/show action
	 * 
	 * @param viewer
	 *            The outline tree viewer
	 * @param productSystemNode
	 *            The product system node of the graphical viewer
	 * @param show
	 *            Indicates if this action should show or hide a specific
	 *            process
	 */
	public HideShowAction(final TreeViewer viewer,
			final ProductSystemNode productSystemNode, final boolean show) {
		this.viewer = viewer;
		this.show = show;
		this.productSystemNode = productSystemNode;
	}

	/**
	 * Disposes the action
	 */
	public void dispose() {
		viewer = null;
	}

	@Override
	public String getId() {
		return "HideShowAction";
	}

	@Override
	public String getText() {
		String text = null;
		if (show) {
			text = Messages.Systems_HideShowAction_ShowText;
		} else {
			text = Messages.Systems_HideShowAction_HideText;
		}
		return text;
	}

	@Override
	public void run() {
		if (!viewer.getSelection().isEmpty()) {
			boolean canExecute = false;
			for (final Object o : ((StructuredSelection) viewer.getSelection())
					.toArray()) {
				if (o instanceof ProcessTreeEditPart) {
					canExecute = true;
					break;
				}
			}
			if (canExecute) {
				final HideShowCommand command = new HideShowCommand(viewer,
						productSystemNode, show);
				productSystemNode.getEditor().getCommandStack()
						.execute(command);
			}
		}
	}

	/**
	 * Setter of the productSystemNode-field
	 * 
	 * @param productSystemNode
	 *            The product system node of the graphical viewer
	 */
	public void setProductSystemNode(final ProductSystemNode productSystemNode) {
		this.productSystemNode = productSystemNode;
	}
}
