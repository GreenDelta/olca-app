package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.ProcessLinks;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class RemoveAllConnectionsAction extends EditorAction {

	private List<ProcessNode> processNodes = new ArrayList<>();

	RemoveAllConnectionsAction() {
		setId(ActionIds.REMOVE_ALL_CONNECTIONS);
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
			List<ProcessLink> processLinks = ProcessLinks.getAll(productSystem,
					processNode.getProcess().getId());
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
		processNodes = getMultiSelectionOfType(selection, ProcessNode.class);
		return processNodes.size() > 0;
	}
}
