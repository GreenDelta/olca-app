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
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLink;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;

/**
 * Deletes the given {@link ConnectionLink}, removes it from the
 * {@link ProductSystemNode} and removes the process link from the product
 * system
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessLinkDeleteCommand extends Command {

	/**
	 * Indicates if the parent process of the source node has more links
	 */
	private boolean hasNoMoreLeftLinks = true;

	/**
	 * Indicates if the parent process of the target node has more links
	 */
	private boolean hasNoMoreRightLinks = true;

	/**
	 * The selected {@link ConnectionLink}
	 */
	private final ConnectionLink link;

	/**
	 * Indicates if the link was visible
	 */
	private boolean linkWasVisible = false;

	/**
	 * Constructor of a new ProcessLinkDeleteCommand
	 * 
	 * @param link
	 *            - the {@link ConnectionLink} which should be deleted
	 */
	public ProcessLinkDeleteCommand(final ConnectionLink link) {
		this.link = link;
	}

	@Override
	public boolean canExecute() {
		return link != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		linkWasVisible = link.getFigure().isVisible();
		this.link.unlink();
		((ProductSystemNode) link.getSourceNode().getParentProcessNode()
				.getParent()).getProductSystem().remove(link.getProcessLink());
		int i = 0;
		ExchangeNode[] exchangeNodes = link.getSourceNode()
				.getParentProcessNode().getExchangeNodes();
		while (hasNoMoreRightLinks && i < exchangeNodes.length) {
			int j = 0;
			while (hasNoMoreRightLinks
					&& j < exchangeNodes[i].getLinks().size()) {
				if (exchangeNodes[i]
						.getLinks()
						.get(j)
						.getSourceNode()
						.getParentProcessNode()
						.getProcess()
						.getId()
						.equals(link.getSourceNode().getParentProcessNode()
								.getProcess().getId())) {
					hasNoMoreRightLinks = false;
				}
				j++;
			}
			i++;
		}
		if (hasNoMoreRightLinks) {
			link.getSourceNode().getParentProcessNode().getFigure()
					.setExpandCollapseFigureVisible(false, false);
		}
		i = 0;
		exchangeNodes = link.getTargetNode().getParentProcessNode()
				.getExchangeNodes();
		while (hasNoMoreLeftLinks && i < exchangeNodes.length) {
			int j = 0;
			while (hasNoMoreLeftLinks && j < exchangeNodes[i].getLinks().size()) {
				if (exchangeNodes[i]
						.getLinks()
						.get(j)
						.getTargetNode()
						.getParentProcessNode()
						.getProcess()
						.getId()
						.equals(link.getTargetNode().getParentProcessNode()
								.getProcess().getId())) {
					hasNoMoreLeftLinks = false;
				}
				j++;
			}
			i++;
		}
		if (hasNoMoreLeftLinks) {
			link.getTargetNode().getParentProcessNode().getFigure()
					.setExpandCollapseFigureVisible(true, false);
		}
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessLinkDeleteCommand_Text;
	}

	@Override
	public void redo() {
		((ProductSystemNode) link.getSourceNode().getParentProcessNode()
				.getParent()).getProductSystem().remove(link.getProcessLink());
		this.link.unlink();
		if (hasNoMoreRightLinks) {
			link.getSourceNode().getParentProcessNode().getFigure()
					.setExpandCollapseFigureVisible(false, false);
		}
		if (hasNoMoreLeftLinks) {
			link.getTargetNode().getParentProcessNode().getFigure()
					.setExpandCollapseFigureVisible(true, false);
		}
	}

	@Override
	public void undo() {
		((ProductSystemNode) link.getSourceNode().getParentProcessNode()
				.getParent()).getProductSystem().add(link.getProcessLink());
		link.link();
		link.getFigure().setVisible(linkWasVisible);
		link.getSourceNode().getParentProcessNode().getFigure()
				.setExpandCollapseFigureVisible(false, true);
		link.getTargetNode().getParentProcessNode().getFigure()
				.setExpandCollapseFigureVisible(true, true);
	}

}
