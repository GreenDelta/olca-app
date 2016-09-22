package org.openlca.app.editors.graphical.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;

public class DeleteLinkCommand extends Command {

	private final List<Link> links;
	private final Map<String, Boolean> visibilityMap = new HashMap<String, Boolean>();

	public DeleteLinkCommand(Link link) {
		this(Collections.singletonList(link));
	}

	public DeleteLinkCommand(List<Link> links) {
		this.links = links;
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
		ProductSystemNode systemNode = links.get(0).sourceNode.parent();
		for (Link link : links) {
			visibilityMap.put(getUniqueId(link), link.isVisible());
			systemNode.getProductSystem().getProcessLinks().remove(link.processLink);
			systemNode.linkSearch.remove(link.processLink);
			link.unlink();
		}
		systemNode.editor.setDirty(true);
	}

	private String getUniqueId(Link connection) {
		ProcessLink link = connection.processLink;
		return link.providerId + "->" + link.flowId + "->" + link.processId + "->" + link.exchangeId;
	}

	@Override
	public String getLabel() {
		return M.DeleteProcesslink;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = links.get(0).sourceNode.parent();
		for (Link link : links) {
			link.unlink();
			systemNode.getProductSystem().getProcessLinks().remove(link.processLink);
			systemNode.linkSearch.remove(link.processLink);
		}
		systemNode.editor.setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = links.get(0).sourceNode.parent();
		for (Link link : links) {
			systemNode.getProductSystem().getProcessLinks().add(link.processLink);
			systemNode.linkSearch.put(link.processLink);
			link.link();
			link.setVisible(visibilityMap.get(getUniqueId(link)));
		}
		systemNode.editor.setDirty(true);
	}

}
