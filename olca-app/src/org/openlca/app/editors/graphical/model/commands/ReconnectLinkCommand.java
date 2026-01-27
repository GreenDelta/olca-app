package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.ProviderType;

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
	public void execute() {
		graph.removeLink(oldLink.processLink);

		var pLink = new ProcessLink();
		pLink.providerId = sourceNode.descriptor.id;
		pLink.providerType = ProviderType.of(sourceNode.descriptor.type);
		pLink.flowId = oldLink.processLink.flowId;
		pLink.processId = targetItem.getNode().descriptor.id;
		pLink.exchangeId = targetItem.exchange.id;

		system.processLinks.add(pLink);
		graph.linkSearch.rebuild(system.processLinks);
		link = new GraphLink(pLink, sourceNode, targetItem);
		graph.mapProcessLinkToGraphLink.put(pLink, link);

		graph.editor.setDirty();
	}

	@Override
	public String getLabel() {
		return M.ReconnectProcessLink;
	}

	@Override
	public void redo() {
		graph.removeLink(oldLink.processLink);
		system.processLinks.add(link.processLink);
		graph.linkSearch.rebuild(system.processLinks);
		graph.mapProcessLinkToGraphLink.put(link.processLink, link);
		link.reconnect();
		graph.editor.setDirty();
	}

	@Override
	public void undo() {
		graph.removeLink(link.processLink);
		system.processLinks.add(oldLink.processLink);
		graph.linkSearch.rebuild(system.processLinks);
		graph.mapProcessLinkToGraphLink.put(oldLink.processLink, oldLink);
		oldLink.reconnect();
		graph.editor.setDirty();
	}

}
