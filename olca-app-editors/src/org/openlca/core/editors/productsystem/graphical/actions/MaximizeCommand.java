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
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;

/**
 * Maximizes the process figure of the given ProcessNode
 * 
 * @author Sebastian Greve
 * 
 */
public class MaximizeCommand extends Command {

	/**
	 * The selected {@link ProcessNode}
	 */
	private final ProcessNode node;

	/**
	 * Constructor of a new MaximizeCommand
	 * 
	 * @param node
	 *            the {@link ProcessNode} which should be maximized
	 */
	public MaximizeCommand(final ProcessNode node) {
		this.node = node;
	}

	@Override
	public boolean canExecute() {
		return node != null && node.isMinimized();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		node.setMinimized(false);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_MaximizeCommand_Text;
	}

	@Override
	public void redo() {
		node.setMinimized(false);
	}

	@Override
	public void undo() {
		node.setMinimized(true);
	}

}
