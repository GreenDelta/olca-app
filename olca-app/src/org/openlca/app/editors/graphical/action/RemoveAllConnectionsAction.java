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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class RemoveAllConnectionsAction extends EditorAction {

	private List<ProcessNode> processNodes = new ArrayList<>();

	RemoveAllConnectionsAction() {
		setId(ActionIds.REMOVE_ALL_CONNECTIONS_ACTION_ID);
		setText(Messages.Systems_RemoveAllConnectionsAction_Text);
	}

	private Command getDeleteCommand(ProcessNode node,
			List<ConnectionLink> links) {
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

	@Override
	public void run() {
		if (processNodes.size() == 0)
			return;
		Command chainCommand = null;
		ProductSystem productSystem = processNodes.get(0).getParent()
				.getProductSystem();
		List<ConnectionLink> links = new ArrayList<>();
		for (ProcessNode processNode : processNodes) {
			ProcessLink[] processLinks = productSystem
					.getProcessLinks(processNode.getProcess().getId());
			for (ProcessLink link : processLinks)
				productSystem.getProcessLinks().remove(link);

			Command cmd = getDeleteCommand(processNode, links);
			if (cmd != null)
				if (chainCommand == null)
					chainCommand = cmd;
				else
					chainCommand = chainCommand.chain(cmd);
		}

		processNodes.get(0).getParent().getEditor().getCommandStack()
				.execute(chainCommand);
	}

	@Override
	protected boolean accept(ISelection selection) {
		processNodes = new ArrayList<>();
		if (selection == null)
			return false;
		if (selection.isEmpty())
			return false;
		if (!(selection instanceof IStructuredSelection))
			return false;

		IStructuredSelection sel = (IStructuredSelection) selection;
		for (Object o : sel.toArray())
			if (o instanceof EditPart) {
				Object model = ((EditPart) o).getModel();
				if (model instanceof ProcessNode)
					processNodes.add((ProcessNode) model);
			}
		return processNodes.size() > 0;
	}
}
