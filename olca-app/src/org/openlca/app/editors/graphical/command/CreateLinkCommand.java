package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class CreateLinkCommand extends Command {

	public boolean startedFromSource;
	public ExchangeNode providerNode;
	public ExchangeNode exchangeNode;

	private ConnectionLink link;
	private ProcessLink processLink;

	@Override
	public boolean canExecute() {
		return providerNode != null && exchangeNode != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		ProductSystemNode systemNode = GraphUtil.getSystemNode(providerNode);
		ProductSystem system = systemNode.getProductSystem();
		processLink = getProcessLink();
		system.getProcessLinks().add(processLink);
		systemNode.getLinkSearch().put(processLink);
		link = getLink();
		link.link();
		systemNode.getEditor().setDirty(true);
	}

	private ProcessLink getProcessLink() {
		if (processLink == null)
			processLink = new ProcessLink();
		if (exchangeNode != null)
			processLink.processId = GraphUtil.getProcess(exchangeNode).getId();
		if (providerNode != null)
			processLink.providerId = GraphUtil.getProcess(providerNode).getId();
		processLink.flowId = GraphUtil.getFlow(exchangeNode).getId();
		processLink.exchangeId = exchangeNode.getExchange().getId();
		return processLink;
	}

	@Override
	public String getLabel() {
		return M.CreateProcesslink;
	}

	@Override
	public void redo() {
		// maybe nodes where deleted before and added again, therefore the
		// (maybe) new instances need to be fetched
		refreshNodes();
		execute();
	}

	private void refreshNodes() {
		// TODO: find exchange nodes in product system
		// ProductSystemNode system = GraphUtil.getSystemNode(providerNode);
		// providerNode = system.getProcessNode(link.sourceNode
		// .getProcess().getId());
		// exchangeNode = system.getProcessNode(link.targetNode
		// .getProcess().getId());
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = GraphUtil.getSystemNode(providerNode);
		ProductSystem system = systemNode.getProductSystem();
		link.unlink();
		system.getProcessLinks().remove(processLink);
		systemNode.getLinkSearch().remove(processLink);
		systemNode.getEditor().setDirty(true);
	}

	public ConnectionLink getLink() {
		if (link == null)
			link = new ConnectionLink();
		link.processLink = getProcessLink();
		link.sourceNode = GraphUtil.getProcessNode(providerNode);
		link.targetNode = GraphUtil.getProcessNode(exchangeNode);
		return link;
	}

}
