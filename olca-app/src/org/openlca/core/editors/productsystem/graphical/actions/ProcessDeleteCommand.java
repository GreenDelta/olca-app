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

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;

/**
 * Deletes the given {@link ProcessNode}, removes it from the
 * {@link ProductSystemNode} and removes the process descriptor from the product
 * system
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessDeleteCommand extends Command {

	/**
	 * The old layout constraints
	 */
	private Rectangle oldLayout;

	/**
	 * The selected {@link ProcessNode}
	 */
	private ProcessNode processNode;

	/**
	 * The {@link ProductSystemNode} of this editor
	 */
	private ProductSystemNode productSystemNode;

	/**
	 * Indicates if the process node was expanded to the left
	 */
	private boolean wasExpandedLeft;

	/**
	 * Indicates if the process node was expanded to the right
	 */
	private boolean wasExpandedRight;

	@Override
	public boolean canExecute() {
		boolean canExecute = processNode != null
				&& productSystemNode != null
				&& (productSystemNode.getProductSystem().getReferenceProcess() == null || !productSystemNode
						.getProductSystem().getReferenceProcess().getId()
						.equals(processNode.getProcess().getId()));
		if (processNode != null)
			for (ExchangeNode node : processNode.getExchangeNodes()) {
				if (node.getLinks().size() > 0) {
					canExecute = false;
					break;
				}
			}
		return canExecute;
	}

	@Override
	public void execute() {
		wasExpandedLeft = processNode.getFigure().isExpandedLeft();
		wasExpandedRight = processNode.getFigure().isExpandedRight();
		oldLayout = processNode.getXyLayoutConstraints();
		productSystemNode.getProductSystem().remove(processNode.getProcess());
		productSystemNode.removeChild(processNode);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessDeleteCommand_Text;
	}

	@Override
	public void redo() {
		productSystemNode.getProductSystem().remove(processNode.getProcess());
		productSystemNode.removeChild(processNode);
	}

	/**
	 * Setter of the processNode-field
	 * 
	 * @param p
	 *            The process node to be deleted
	 */
	public void setProcessNode(final ProcessNode p) {
		processNode = p;
	}

	/**
	 * Setter of productSystemNode-field
	 * 
	 * @param p
	 *            The product system node of the graphical viewer
	 */
	public void setProductSystemNode(final ProductSystemNode p) {
		productSystemNode = p;
	}

	@Override
	public void undo() {
		productSystemNode.addChild(processNode);
		processNode.setXyLayoutConstraints(oldLayout);
		productSystemNode.getProductSystem().add(processNode.getProcess());
		processNode.getFigure().setExpandedLeft(wasExpandedLeft);
		processNode.getFigure().setExpandedRight(wasExpandedRight);
	}
}
