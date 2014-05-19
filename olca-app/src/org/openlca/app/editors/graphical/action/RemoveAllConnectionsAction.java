package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;

class RemoveAllConnectionsAction extends EditorAction {

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
		ProductSystemNode systemNode = getEditor().getModel();
		List<ConnectionLink> links = new ArrayList<>();
		// create new link search to avoid problems with missing entries before
		// ConnectionLink.unlink is called
		MutableProcessLinkSearchMap linkSearch = new MutableProcessLinkSearchMap(
				systemNode.getProductSystem().getProcessLinks());
		for (ProcessNode processNode : processNodes) {
			List<ProcessLink> processLinks = linkSearch.getLinks(processNode
					.getProcess().getId());
			for (ProcessLink link : processLinks)
				linkSearch.remove(link);
			Command cmd = getDeleteCommand(processNode, links);
			if (cmd != null)
				if (chainCommand == null)
					chainCommand = cmd;
				else
					chainCommand = chainCommand.chain(cmd);
		}
		getEditor().getCommandStack().execute(chainCommand);
	}

	@Override
	protected boolean accept(ISelection selection) {
		processNodes = getMultiSelectionOfType(selection, ProcessNode.class);
		if (processNodes.size() == 0)
			return false;
		for (ProcessNode node : processNodes)
			if (node.getLinks().size() > 0)
				return true;
		return false;
	}
}
