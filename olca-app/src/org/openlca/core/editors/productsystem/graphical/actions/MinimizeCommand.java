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

/**
 * Minimizes the process figure
 * 
 * @author Sebastian Greve
 * 
 */
public class MinimizeCommand extends Command {

	/**
	 * The selected {@link ProcessNode}
	 */
	private final ProcessNode node;

	/**
	 * Constructor of a new MinimizeCommand
	 * 
	 * @param node
	 *            the {@link ProcessNode} which should be minimized
	 */
	public MinimizeCommand(final ProcessNode node) {
		this.node = node;
	}

	@Override
	public boolean canExecute() {
		return node != null && !node.isMinimized();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		node.setMinimized(true);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_MinimizeCommand_Text;
	}

	@Override
	public void redo() {
		node.setMinimized(true);
	}

	@Override
	public void undo() {
		node.setMinimized(false);
	}

}
