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

import org.eclipse.jface.action.Action;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProductSystem;

/**
 * Marks a process in the graph
 * 
 * @author Sebastian Greve
 * 
 */
public class MarkProcessAction extends Action {

	/**
	 * The id of the process
	 */
	public static final String ID = "org.openlca.core.editors.productsystem.graphical.actions.MarkProcessAction";

	/**
	 * Indicates if the process nodes should be marked or unmarked
	 */
	private final boolean mark;

	/**
	 * The process nodes to mark/unmark
	 */
	public ProcessNode[] nodes;

	/**
	 * Creates a new instance
	 * 
	 * @param mark
	 *            Indicates if the process nodes should be marked or unmarked
	 */
	public MarkProcessAction(final boolean mark) {
		this.mark = mark;
	}

	@Override
	public String getId() {
		return ID + mark;
	}

	@Override
	public String getText() {
		return mark ? Messages.Systems_MarkProcess
				: Messages.Systems_UnmarkProcess;
	}

	@Override
	public void run() {
		if (nodes != null && nodes.length > 0) {
			final ProductSystem ps = ((ProductSystemNode) nodes[0].getParent())
					.getProductSystem();
			for (final ProcessNode node : nodes) {
				if (mark) {
					ps.mark(node.getProcess().getId());
				} else {
					ps.unmark(node.getProcess().getId());
				}
			}
		}
	}

	/**
	 * Setter of the process nodes
	 * 
	 * @param nodes
	 *            The nodes to mark/unmark
	 */
	public void setNodes(final ProcessNode[] nodes) {
		this.nodes = nodes;
	}

}
