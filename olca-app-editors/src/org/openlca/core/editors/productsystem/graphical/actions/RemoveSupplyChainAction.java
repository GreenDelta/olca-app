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
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

/**
 * Action for removing the supply chain of a process
 * 
 * @author Sebastian Greve
 * 
 */
public class RemoveSupplyChainAction extends Action {

	/**
	 * The id of the action
	 */
	public final static String ID = "org.openlca.core.editors.productsystem.graphical.actions.RemoveSupplyChainAction";

	/**
	 * The process node to remove the supply chain from
	 */
	private ProcessNode processNode;

	/**
	 * Creates a new instance
	 */
	public RemoveSupplyChainAction() {
		setId(ID);
	}

	/**
	 * Removes the supply chain from the given process
	 * 
	 * @param process
	 *            The process to remove the supply chain from
	 * @param productSystem
	 *            The product system containing the processes
	 */
	private void removeSupplyChain(final Process process,
			final ProductSystem productSystem) {
		final ProcessLink[] links = productSystem.getIncomingLinks(process
				.getId());

		// for each process link
		for (final ProcessLink link : links) {
			// remove from product system
			productSystem.remove(link);

			final ProcessLink[] providerLinks = productSystem
					.getOutgoingLinks(link.getProviderProcess().getId());
			if (providerLinks.length == 0) {
				removeSupplyChain(link.getProviderProcess(), productSystem);
				productSystem.remove(link.getProviderProcess());
			}
		}
	}

	@Override
	public String getText() {
		return Messages.Systems_RemoveSupplyChainAction_Text;
	}

	@Override
	public void run() {
		final Process process = processNode.getProcess();
		final ProductSystemNode productSystemNode = (ProductSystemNode) processNode
				.getParent();
		final ProductSystem productSystem = productSystemNode
				.getProductSystem();
		productSystem.removePropertyChangeListener(productSystemNode
				.getEditor().getOutline());
		removeSupplyChain(process, productSystem);
		productSystemNode.getEditor().reset(null);
		productSystem.addPropertyChangeListener(productSystemNode.getEditor()
				.getOutline());
		productSystemNode.getEditor().getOutline().refresh();
	}

	/**
	 * Setter of the process node
	 * 
	 * @param processNode
	 *            The process node to remove the supply chain from
	 */
	public void setProcessNode(final ProcessNode processNode) {
		this.processNode = processNode;
	}
}
