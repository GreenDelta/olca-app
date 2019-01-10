package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.command.DeleteLinkCommand;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;

class RemoveAllConnectionsAction extends EditorAction {

	private List<ProcessNode> processNodes = new ArrayList<>();

	RemoveAllConnectionsAction() {
		setId(ActionIds.REMOVE_ALL_CONNECTIONS);
		setText(M.RemoveConnections);
	}

	@Override
	public void run() {
		if (processNodes.size() == 0)
			return;
		ProductSystemNode systemNode = editor.getModel();
		List<Link> links = new ArrayList<>();
		// create new link search to avoid problems with missing entries before
		// ConnectionLink.unlink is called
		List<ProcessLink> pLinks = systemNode.getProductSystem().processLinks;
		MutableProcessLinkSearchMap linkSearch = new MutableProcessLinkSearchMap(pLinks);
		for (ProcessNode processNode : processNodes) {
			List<ProcessLink> processLinks = linkSearch.getLinks(processNode.process.id);
			for (ProcessLink link : processLinks)
				linkSearch.remove(link);
			for (Link link : processNode.links)
				if (!links.contains(link))
					links.add(link);
		}
		Command command = new DeleteLinkCommand(links);
		editor.getCommandStack().execute(command);
	}

	@Override
	protected boolean accept(ISelection selection) {
		processNodes = getMultiSelectionOfType(selection, ProcessNode.class);
		if (processNodes.size() == 0)
			return false;
		for (ProcessNode node : processNodes)
			if (node.links.size() > 0)
				return true;
		return false;
	}
}
