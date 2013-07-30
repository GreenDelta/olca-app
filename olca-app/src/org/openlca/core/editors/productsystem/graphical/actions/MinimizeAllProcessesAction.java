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

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.ProductSystemGraphEditor;
import org.openlca.core.editors.productsystem.graphical.model.Node;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;

/**
 * Creates a command chain with {@link MinimizeCommand}'s for each
 * {@link ProcessNode} and executes it
 * 
 * @see Action
 * 
 * @author Sebastian Greve
 * 
 */
public class MinimizeAllProcessesAction extends Action {

	/**
	 * ID of this Action
	 */
	public static final String ID = "org.openlca.core.editors.productsystem.graphical.actions.MinimizeAllProcessesAction";

	/**
	 * The graphical editor
	 */
	private ProductSystemGraphEditor productSystemEditor;

	/**
	 * Disposes the action
	 */
	public void dispose() {
		productSystemEditor = null;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.MINIMIZE_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.Systems_MinimizeAllProcessesAction_Text;
	}

	@Override
	public void run() {
		Command actualCommand = null;
		// for each process node
		for (final Node node : productSystemEditor.getModel()
				.getChildrenArray()) {
			if (node instanceof ProcessNode) {
				final ProcessNode processNode = (ProcessNode) node;
				if (!processNode.isMinimized()) {
					// minimize
					if (actualCommand == null) {
						actualCommand = new MinimizeCommand(processNode);
					} else {
						actualCommand = actualCommand
								.chain(new MinimizeCommand(processNode));
					}
				}
			}
		}
		if (actualCommand != null) {
			productSystemEditor.getCommandStack().execute(actualCommand);
		}
	}

	/**
	 * Setter of the productSystemEditor-field
	 * 
	 * @param productSystemEditor
	 *            The graphical editor
	 */
	public void setProductSystemEditor(
			final ProductSystemGraphEditor productSystemEditor) {
		this.productSystemEditor = productSystemEditor;
	}

}
