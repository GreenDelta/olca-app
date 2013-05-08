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
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLink;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;

/**
 * Creates a new {@link ConnectionLink}, adds it to the {@link #sourceNode} and
 * {@link #targetNode} and adds the process link to the product system
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessLinkCreateCommand extends Command {

	/**
	 * The new {@link ConnectionLink}
	 */
	private ConnectionLink link;

	/**
	 * The represented process link
	 */
	private ProcessLink processLink;

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
		return sourceNode != null && targetNode != null
				&& sourceNode.matches(targetNode);
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		final ProductSystemNode psNode = (ProductSystemNode) sourceNode
				.getParentProcessNode().getParent();
		processLink = new ProcessLink();
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
		sourceNode
				.getParentProcessNode()
				.getFigure()
				.setExpandCollapseFigureVisible(
						sourceNode.getExchange().isInput(), true);
		targetNode
				.getParentProcessNode()
				.getFigure()
				.setExpandCollapseFigureVisible(
						targetNode.getExchange().isInput(), true);
		psNode.getProductSystem().add(processLink);
		link = new ConnectionLink(sourceNode, targetNode, processLink);
		link.link();
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessLinkCreateCommand_Text;
	}

	/**
	 * Getter of the sourceNode-field
	 * 
	 * @return The source node of the new connection
	 */
	public ExchangeNode getSourceNode() {
		return sourceNode;
	}

	/**
	 * Getter of the targetNode-field
	 * 
	 * @return The target node of the new connection
	 */
	public ExchangeNode getTargetNode() {
		return targetNode;
	}

	@Override
	public void redo() {
		link.link();
		final ProductSystemNode psNode = (ProductSystemNode) sourceNode
				.getParentProcessNode().getParent();
		psNode.getProductSystem().add(processLink);
		sourceNode
				.getParentProcessNode()
				.getFigure()
				.setExpandCollapseFigureVisible(
						sourceNode.getExchange().isInput(), true);
		targetNode
				.getParentProcessNode()
				.getFigure()
				.setExpandCollapseFigureVisible(
						targetNode.getExchange().isInput(), true);
	}

	/**
	 * Setter of the sourceNode-field
	 * 
	 * @param sourceNode
	 *            The source node of the new connection
	 */
	public void setSourceNode(final ExchangeNode sourceNode) {
		this.sourceNode = sourceNode;
	}

	/**
	 * Setter of the targetNode-field
	 * 
	 * @param targetNode
	 *            The target node of the new connection
	 */
	public void setTargetNode(final ExchangeNode targetNode) {
		this.targetNode = targetNode;
	}

	@Override
	public void undo() {
		final ProductSystemNode psNode = (ProductSystemNode) sourceNode
				.getParentProcessNode().getParent();
		psNode.getProductSystem().remove(processLink);
		link.unlink();
		sourceNode
				.getParentProcessNode()
				.getFigure()
				.setExpandCollapseFigureVisible(
						sourceNode.getExchange().isInput(), false);
		targetNode
				.getParentProcessNode()
				.getFigure()
				.setExpandCollapseFigureVisible(
						targetNode.getExchange().isInput(), false);
	}

}
