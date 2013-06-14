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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLink;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

/**
 * Creates a command chain with {@link ProcessLinkDeleteCommand}'s for each
 * {@link ConnectionLink} of each selected {@link ProcessNode} and executes it
 * 
 * @see Action
 * 
 * @author Sebastian Greve
 * 
 */
public class RemoveConnectionsFromProcessAction extends Action {

	/**
	 * ID of this {@link Action}
	 */
	public static final String ID = "org.openlca.core.editors.productsystem.graphical.actions.RemoveConnectionsFromProcessAction";

	/**
	 * Collects all links for which a delete command was created, so no link is
	 * delete twice (if provider process and recipient process is selected)
	 */
	private final List<ConnectionLink> links = new ArrayList<>();

	/**
	 * Selected {@link ProcessNode}s
	 */
	private ProcessNode[] processNodes;

	/**
	 * {@link ISelection}
	 */
	private IStructuredSelection selection;

	/**
	 * {@link PropertyChangeSupport}
	 */
	private final PropertyChangeSupport support = new PropertyChangeSupport(
			this);

	/**
	 * Creates the {@link ProcessLinkDeleteCommand} chain for the given
	 * {@link ProcessNode}
	 * 
	 * @param processNode
	 *            the ProcessNode to be deleted
	 * @return The delete command (or command chain)
	 */
	private Command getDeleteCommand(final ProcessNode processNode) {
		Command chainCommand = null;
		// for each exchange node
		for (final ExchangeNode node : processNode.getExchangeNodes()) {
			// for each connection link
			for (final ConnectionLink link : node.getLinks()) {
				if (!links.contains(link)) {
					// create delete command
					if (chainCommand == null) {
						chainCommand = new ProcessLinkDeleteCommand(link);
					} else {
						chainCommand = chainCommand
								.chain(new ProcessLinkDeleteCommand(link));
					}
					links.add(link);
				}
			}
		}
		return chainCommand;
	}

	/**
	 * Adds a property change listener to the support
	 * 
	 * @param listener
	 *            The property change listener to be added
	 */
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	/**
	 * Disposes the action
	 */
	public void dispose() {
		if (links != null) {
			links.clear();
		}
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getText() {
		return Messages.Systems_RemoveConnectionsFromProcessAction_Text;
	}

	/**
	 * Removes a property change listener from the support
	 * 
	 * @param listener
	 *            The property change listener to be removed
	 */
	public void removePropertyChangeListener(
			final PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	@Override
	public void run() {
		if (processNodes.length > 0) {
			Command chainCommand = null;
			ProductSystem productSystem = null;

			// for each process node
			for (final ProcessNode processNode : processNodes) {
				if (productSystem == null) {
					// get product system
					productSystem = ((ProductSystemNode) processNode
							.getParent()).getProductSystem();
				}
				final ProcessLink[] processLinks = productSystem
						.getProcessLinks(processNode.getProcess().getId());
				// for each process link
				for (final ProcessLink link : processLinks) {
					productSystem.remove(link);
				}

				final Command cmd = getDeleteCommand(processNode);
				if (cmd != null) {
					if (chainCommand == null) {
						chainCommand = cmd;
					} else {
						chainCommand = chainCommand.chain(cmd);
					}
				}
			}

			((ProductSystemNode) processNodes[0].getParent()).getEditor()
					.getCommandStack().execute(chainCommand);
		}
		links.clear();
		support.firePropertyChange("RemovedConnections", null, selection);
	}

	/**
	 * Setter of the processNodes-field
	 * 
	 * @param processNodes
	 *            the selected ProcessNodes
	 */
	public void setProcessNodes(final ProcessNode[] processNodes) {
		this.processNodes = processNodes;
	}

	/**
	 * Setter of the selection-field
	 * 
	 * @param selection
	 *            The actual selection
	 */
	public void setSelection(final IStructuredSelection selection) {
		this.selection = selection;
	}

}
