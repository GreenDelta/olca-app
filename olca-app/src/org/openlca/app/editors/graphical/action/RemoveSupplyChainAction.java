/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class RemoveSupplyChainAction extends Action {

	private ProcessNode node;

	RemoveSupplyChainAction() {
		setId(ActionIds.REMOVE_SUPPLY_CHAIN_ACTION_ID);
		setText(Messages.Systems_RemoveSupplyChainAction_Text);
	}

	@Override
	public void run() {
		ProcessDescriptor process = node.getProcess();
		ProductSystemNode productSystemNode = node.getParent();
		ProductSystem productSystem = productSystemNode.getProductSystem();
		removeSupplyChain(process.getId(), productSystem);
		// TODO adjust
		// productSystemNode.getEditor().reset(null);
		productSystemNode.getEditor().getOutline().refresh();
	}

	private void removeSupplyChain(long processId, ProductSystem productSystem) {
		ProcessLink[] links = productSystem.getIncomingLinks(processId);
		for (ProcessLink link : links) {
			productSystem.getProcessLinks().remove(link);
			ProcessLink[] providerLinks = productSystem.getOutgoingLinks(link
					.getProviderId());
			if (providerLinks.length == 0) {
				removeSupplyChain(link.getProviderId(), productSystem);
				productSystem.getProcessLinks().remove(link.getProviderId());
			}
		}
	}

	void setNode(ProcessNode node) {
		this.node = node;
	}
}
