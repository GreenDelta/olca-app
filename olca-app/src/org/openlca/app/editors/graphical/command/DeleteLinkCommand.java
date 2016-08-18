package org.openlca.app.editors.graphical.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphUtil;
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
	public String getLabel() {
		return M.DeleteProcesslink;
	}

	@Override
	public void execute() {
		run((systemNode, link) -> {
			visibilityMap.put(link.processLink.exchangeId, link.isVisible());
			systemNode.getProductSystem().getProcessLinks()
					.remove(link.processLink);
			systemNode.getLinkSearch().remove(link.processLink);
			link.unlink();
		});
	}

	@Override
	public void redo() {
		run((systemNode, link) -> {
			link.unlink();
			systemNode.getProductSystem().getProcessLinks()
					.remove(link.processLink);
			systemNode.getLinkSearch().remove(link.processLink);
		});
	}

	@Override
	public void undo() {
		run((systemNode, link) -> {
			systemNode.getProductSystem().getProcessLinks()
					.add(link.processLink);
			systemNode.getLinkSearch().put(link.processLink);
			link.link();
			link.setVisible(visibilityMap.get(link.processLink.exchangeId));
		});
	}

	private void run(BiConsumer<ProductSystemNode, ConnectionLink> fn) {
		ProductSystemNode systemNode = GraphUtil.getSystemNode(
				links.get(0).provider);
		for (ConnectionLink link : links) {
			fn.accept(systemNode, link);
		}
		systemNode.getEditor().setDirty(true);
	}

	void setLinks(List<ConnectionLink> links) {
		this.links = links;
	}

}
