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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class RemoveAllConnectionsAction extends Action {

	private List<ConnectionLink> links = new ArrayList<>();
	private ProcessNode[] processNodes;
	private IStructuredSelection selection;
	private PropertyChangeSupport support = new PropertyChangeSupport(this);

	RemoveAllConnectionsAction() {
		setId(ActionIds.REMOVE_ALL_CONNECTIONS_ACTION_ID);
		setText(Messages.Systems_RemoveAllConnectionsAction_Text);
	}

	private Command getDeleteCommand(ProcessNode node) {
		Command chainCommand = null;
		for (ConnectionLink link : node.getLinks()) {
			if (!links.contains(link)) {
				if (chainCommand == null)
					chainCommand = CommandFactory.createDeleteLinkCommand(link);
				else
					chainCommand = chainCommand.chain(CommandFactory
							.createDeleteLinkCommand(link));
				links.add(link);
			}
		}
		return chainCommand;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	@Override
	public void run() {
		if (processNodes.length == 0)
			return;
		Command chainCommand = null;
		ProductSystem productSystem = processNodes[0].getParent()
				.getProductSystem();

		for (ProcessNode processNode : processNodes) {
			ProcessLink[] processLinks = productSystem
					.getProcessLinks(processNode.getProcess().getId());
			for (ProcessLink link : processLinks)
				productSystem.getProcessLinks().remove(link);

			Command cmd = getDeleteCommand(processNode);
			if (cmd != null)
				if (chainCommand == null)
					chainCommand = cmd;
				else
					chainCommand = chainCommand.chain(cmd);
		}

		processNodes[0].getParent().getEditor().getCommandStack()
				.execute(chainCommand);

		links.clear();
		support.firePropertyChange("RemovedConnections", null, selection);
	}

	public void setProcessNodes(ProcessNode[] processNodes) {
		this.processNodes = processNodes;
	}

	public void setSelection(IStructuredSelection selection) {
		this.selection = selection;
	}

}
