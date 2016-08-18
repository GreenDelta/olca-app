package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class ReconnectCommand extends Command {

	private ConnectionLink oldLink;
	private ExchangeNode provider;
	private ExchangeNode exchange;

	private ConnectionLink newLink;

	public ReconnectCommand(ConnectionLink oldLink, ExchangeNode provider,
			ExchangeNode exchange) {
		this.oldLink = oldLink;
		this.provider = provider;
		this.exchange = exchange;
	}

	@Override
	public boolean canExecute() {
		return provider != null && exchange != null && oldLink != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		oldLink.unlink();
		ProductSystemNode system = GraphUtil.getSystemNode(provider);
		system.getProductSystem().getProcessLinks().remove(oldLink.processLink);
		system.getLinkSearch().remove(oldLink.processLink);
		ProcessLink processLink = createProcessLink();
		system.getProductSystem().getProcessLinks().add(processLink);
		system.getLinkSearch().put(processLink);
		createConnection(processLink);
		system.getEditor().setDirty(true);
	}

	private ProcessLink createProcessLink() {
		ProcessLink processLink = new ProcessLink();
		processLink.providerId = GraphUtil.getProcess(provider).getId();
		processLink.flowId = GraphUtil.getFlow(provider).getId();
		processLink.processId = GraphUtil.getProcess(exchange).getId();
		processLink.exchangeId = exchange.getExchange().getId();
		return processLink;
	}

	private void createConnection(ProcessLink processLink) {
		newLink = new ConnectionLink();
		newLink.provider = provider;
		newLink.exchange = exchange;
		newLink.processLink = processLink;
		newLink.link();
	}

	@Override
	public String getLabel() {
		return M.ReconnectProcesslink;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = GraphUtil.getSystemNode(provider);
		ProductSystem system = systemNode.getProductSystem();
		oldLink.unlink();
		system.getProcessLinks().remove(oldLink.processLink);
		systemNode.getLinkSearch().remove(oldLink.processLink);
		system.getProcessLinks().add(newLink.processLink);
		systemNode.getLinkSearch().put(newLink.processLink);
		newLink.link();
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = GraphUtil.getSystemNode(provider);
		ProductSystem system = systemNode.getProductSystem();
		newLink.unlink();
		system.getProcessLinks().remove(newLink.processLink);
		systemNode.getLinkSearch().remove(newLink.processLink);
		system.getProcessLinks().add(oldLink.processLink);
		systemNode.getLinkSearch().put(oldLink.processLink);
		oldLink.link();
		systemNode.getEditor().setDirty(true);
	}

}
