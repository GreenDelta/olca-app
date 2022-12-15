package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class ReconnectLinkCommand extends Command {

	private final Node sourceNode;
	private final ExchangeItem targetItem;
	private final GraphLink oldLink;
	private final Graph graph;
	private final ProductSystem system;
	private GraphLink link;

	public ReconnectLinkCommand(Node node, ExchangeItem target, GraphLink link) {
		sourceNode = node;
		targetItem = target;
		oldLink = link;
		graph = node.getGraph();
		system = graph.getProductSystem();
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
		graph.removeLink(oldLink.processLink);

		var processLink = new ProcessLink();
		processLink.providerId = sourceNode.descriptor.id;
		processLink.setProviderType(sourceNode.descriptor.type);
		processLink.flowId = oldLink.processLink.flowId;
		processLink.processId = targetItem.getNode().descriptor.id;
		processLink.exchangeId = targetItem.exchange.id;

		graph.getProductSystem().processLinks.add(processLink);
		graph.linkSearch.put(processLink);
		link = new GraphLink(processLink, sourceNode, targetItem);
		graph.mapProcessLinkToGraphLink.put(processLink, link);

		graph.editor.setDirty();
	}

	@Override
	public String getLabel() {
		return M.ReconnectProcesslink;
	}

	@Override
	public void redo() {
		graph.removeLink(oldLink.processLink);

		system.processLinks.add(link.processLink);
		graph.linkSearch.put(link.processLink);
		graph.mapProcessLinkToGraphLink.put(link.processLink, link);
		link.reconnect();

		graph.editor.setDirty();
	}

	@Override
	public void undo() {
		graph.removeLink(link.processLink);

		system.processLinks.add(oldLink.processLink);
		graph.linkSearch.put(oldLink.processLink);
		graph.mapProcessLinkToGraphLink.put(oldLink.processLink, oldLink);
		oldLink.reconnect();

		graph.editor.setDirty();
	}

}
