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
import org.openlca.app.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;

/**
 * Creates a new {@link ProcessNode}, adds it to the {@link ProductSystemNode}
 * and adds the process descriptor to the product system
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessCreateCommand extends Command {

	/**
	 * The new {@link ProcessNode}
	 */
	private ProcessNode processNode;

	/**
	 * The {@link ProductSystemNode} of this editor
	 */
	private ProductSystemNode productSystemNode;

	@Override
	public boolean canExecute() {
		boolean canExecute = true;
		if (processNode == null
				|| productSystemNode == null
				|| productSystemNode.getProductSystem().getProcess(
						processNode.getProcess().getId()) != null) {
			canExecute = false;
		}
		return canExecute;
	}

	@Override
	public boolean canUndo() {
		if (productSystemNode == null || processNode == null) {
			return false;
		}
		return productSystemNode.contains(processNode);
	}

	@Override
	public void execute() {
		productSystemNode.getProductSystem().add(processNode.getProcess());
		productSystemNode.addChild(processNode);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessCreateCommand_Text;
	}

	@Override
	public void redo() {
		productSystemNode.getProductSystem().add(processNode.getProcess());
		productSystemNode.addChild(processNode);
	}

	/**
	 * Setter of the processNode-field
	 * 
	 * @param p
	 *            The process node to be created
	 */
	public void setProcessNode(final ProcessNode p) {
		processNode = p;
	}

	/**
	 * Setter of the productSystemNode-field
	 * 
	 * @param p
	 *            The product system node of the graphical editor
	 */
	public void setProductSystemNode(final ProductSystemNode p) {
		productSystemNode = p;
	}

	@Override
	public void undo() {
		productSystemNode.getProductSystem().remove(processNode.getProcess());
		productSystemNode.removeChild(processNode);
	}
}
