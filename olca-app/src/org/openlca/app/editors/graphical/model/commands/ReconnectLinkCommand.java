package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.ProcessLink;

public class ReconnectLinkCommand extends Command {

	private final Node sourceNode;
	private final ExchangeItem targetItem;
	private final GraphLink oldLink;
	private GraphLink link;

	public ReconnectLinkCommand(Node sourceNode, ExchangeItem targetItem, GraphLink oldLink) {
		this.sourceNode = sourceNode;
		this.targetItem = targetItem;
		this.oldLink = oldLink;
	}

	@Override
	public boolean canExecute() {
		if (sourceNode == null)
			return false;
		if (targetItem == null)
			return false;
		return oldLink != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		var graph = sourceNode.getGraph();

		oldLink.disconnect();
		graph.getProductSystem().processLinks.remove(oldLink.processLink);
		graph.linkSearch.remove(oldLink.processLink);

		var processLink = new ProcessLink();
		processLink.providerId = sourceNode.descriptor.id;
		processLink.setProviderType(sourceNode.descriptor.type);
		processLink.flowId = oldLink.processLink.flowId;
		processLink.processId = targetItem.getNode().descriptor.id;
		processLink.exchangeId = targetItem.exchange.id;

		graph.getProductSystem().processLinks.add(processLink);
		graph.linkSearch.put(processLink);
		link.setProcessLink(processLink);
		link.reconnect(targetItem, sourceNode);

		graph.editor.setDirty();
	}

	@Override
	public String getLabel() {
		return M.ReconnectProcesslink;
	}

	@Override
	public void redo() {
		var graph = sourceNode.getGraph();
		var system = graph.getProductSystem();

		oldLink.disconnect();

		system.processLinks.remove(oldLink.processLink);
		graph.linkSearch.remove(oldLink.processLink);
		system.processLinks.add(link.processLink);
		graph.linkSearch.put(link.processLink);
		link.reconnect();

		graph.editor.setDirty();
	}

	@Override
	public void undo() {
		var graph = sourceNode.getGraph();
		var system = graph.getProductSystem();
		link.disconnect();

		system.processLinks.remove(link.processLink);
		graph.linkSearch.remove(link.processLink);
		system.processLinks.add(oldLink.processLink);
		graph.linkSearch.put(oldLink.processLink);
		oldLink.reconnect();

		graph.editor.setDirty();
	}

}
