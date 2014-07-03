package org.openlca.app.editors.graphical.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;

public class DeleteLinkCommand extends Command {

	private List<ConnectionLink> links;
	private Map<String, Boolean> visibilityMap = new HashMap<String, Boolean>();

	DeleteLinkCommand() {
	}

	@Override
	public boolean canExecute() {
		return links != null && !links.isEmpty();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		ProductSystemNode systemNode = links.get(0).getSourceNode().getParent();
		for (ConnectionLink link : links) {
			visibilityMap.put(getUniqueId(link), link.isVisible());
			systemNode.getProductSystem().getProcessLinks()
					.remove(link.getProcessLink());
			systemNode.getLinkSearch().remove(link.getProcessLink());
			link.unlink();
		}
		systemNode.getEditor().setDirty(true);
	}

	private String getUniqueId(ConnectionLink connection) {
		ProcessLink link = connection.getProcessLink();
		return link.getProviderId() + "->" + link.getFlowId() + "->"
				+ link.getRecipientId();
	}

	@Override
	public String getLabel() {
		return Messages.DeleteProcesslink;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = links.get(0).getSourceNode().getParent();
		for (ConnectionLink link : links) {
			link.unlink();
			systemNode.getProductSystem().getProcessLinks()
					.remove(link.getProcessLink());
			systemNode.getLinkSearch().remove(link.getProcessLink());
		}
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = links.get(0).getSourceNode().getParent();
		for (ConnectionLink link : links) {
			systemNode.getProductSystem().getProcessLinks()
					.add(link.getProcessLink());
			systemNode.getLinkSearch().put(link.getProcessLink());
			link.link();
			link.setVisible(visibilityMap.get(getUniqueId(link)));
		}
		systemNode.getEditor().setDirty(true);
	}

	void setLinks(List<ConnectionLink> links) {
		this.links = links;
	}

}
