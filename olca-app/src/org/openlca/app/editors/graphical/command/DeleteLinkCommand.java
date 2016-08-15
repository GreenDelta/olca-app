package org.openlca.app.editors.graphical.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class DeleteLinkCommand extends Command {

	private List<ConnectionLink> links;
	private Map<Long, Boolean> visibilityMap = new HashMap<Long, Boolean>();

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
		ProductSystemNode systemNode = links.get(0).sourceNode.getParent();
		for (ConnectionLink link : links) {
			visibilityMap.put(link.processLink.exchangeId, link.isVisible());
			systemNode.getProductSystem().getProcessLinks()
					.remove(link.processLink);
			systemNode.getLinkSearch().remove(link.processLink);
			link.unlink();
		}
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		return M.DeleteProcesslink;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = links.get(0).sourceNode.getParent();
		for (ConnectionLink link : links) {
			link.unlink();
			systemNode.getProductSystem().getProcessLinks()
					.remove(link.processLink);
			systemNode.getLinkSearch().remove(link.processLink);
		}
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = links.get(0).sourceNode.getParent();
		for (ConnectionLink link : links) {
			systemNode.getProductSystem().getProcessLinks()
					.add(link.processLink);
			systemNode.getLinkSearch().put(link.processLink);
			link.link();
			link.setVisible(visibilityMap.get(link.processLink.exchangeId));
		}
		systemNode.getEditor().setDirty(true);
	}

	void setLinks(List<ConnectionLink> links) {
		this.links = links;
	}

}
