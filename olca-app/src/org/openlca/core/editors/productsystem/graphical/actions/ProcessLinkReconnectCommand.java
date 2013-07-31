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

import java.util.UUID;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLink;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;

/**
 * Reconnects the {@link ConnectionLink}, refreshes the
 * {@link ProductSystemNode} and the product system
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessLinkReconnectCommand extends Command {

	/**
	 * The new {@link ConnectionLink}
	 */
	private ConnectionLink link;

	/**
	 * The old {@link ConnectionLink}
	 */
	private ConnectionLink oldLink;

	/**
	 * The source {@link ExchangeNode}
	 */
	private ExchangeNode sourceNode;

	/**
	 * The target {@link ExchangeNode}
	 */
	private ExchangeNode targetNode;

	@Override
	public boolean canExecute() {
		return sourceNode != null && targetNode != null && oldLink != null
				&& sourceNode.matches(targetNode);
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		oldLink.unlink();
		final ProductSystemNode psNode = (ProductSystemNode) sourceNode
				.getParentProcessNode().getParent();
		psNode.getProductSystem().remove(oldLink.getProcessLink());
		final ProcessLink processLink = new ProcessLink();
		processLink.setId(UUID.randomUUID().toString());
		if (sourceNode.getExchange().isInput()) {
			processLink.setRecipientProcess(sourceNode.getParentProcessNode()
					.getProcess());
			processLink.setRecipientInput(sourceNode.getExchange());
			processLink.setProviderProcess(targetNode.getParentProcessNode()
					.getProcess());
			processLink.setProviderOutput(targetNode.getExchange());
		} else {
			processLink.setRecipientProcess(targetNode.getParentProcessNode()
					.getProcess());
			processLink.setRecipientInput(targetNode.getExchange());
			processLink.setProviderProcess(sourceNode.getParentProcessNode()
					.getProcess());
			processLink.setProviderOutput(sourceNode.getExchange());
		}
		psNode.getProductSystem().add(processLink);
		link = new ConnectionLink(sourceNode, targetNode, processLink);
		link.link();
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessLinkReconnectCommand_Text;
	}

	@Override
	public void redo() {
		link.link();
		oldLink.unlink();
		final ProductSystemNode psNode = (ProductSystemNode) sourceNode
				.getParentProcessNode().getParent();
		psNode.getProductSystem().remove(oldLink.getProcessLink());
		psNode.getProductSystem().add(link.getProcessLink());
	}

	/**
	 * Setter of oldLink-field
	 * 
	 * @param oldLink
	 *            The old connection link
	 */
	public void setOldLink(final ConnectionLink oldLink) {
		this.oldLink = oldLink;
	}

	/**
	 * Setter of the sourceNode-field
	 * 
	 * @param sourceNode
	 *            The source node of the connection
	 */
	public void setSourceNode(final ExchangeNode sourceNode) {
		this.sourceNode = sourceNode;
	}

	/**
	 * Setter of the targetNode-field
	 * 
	 * @param targetNode
	 *            The target node of the connection
	 */
	public void setTargetNode(final ExchangeNode targetNode) {
		this.targetNode = targetNode;
	}

	@Override
	public void undo() {
		final ProductSystemNode psNode = (ProductSystemNode) sourceNode
				.getParentProcessNode().getParent();
		psNode.getProductSystem().remove(link.getProcessLink());
		psNode.getProductSystem().add(oldLink.getProcessLink());
		link.unlink();
		oldLink.link();
	}

}
