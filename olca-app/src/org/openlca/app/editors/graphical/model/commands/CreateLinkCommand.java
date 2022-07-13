package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;


public class CreateLinkCommand extends Command {

	public final long flowId;
	public ExchangeItem source, target;
	public boolean startedFromSource;
	private ProcessLink processLink;
	private Link link;

	public CreateLinkCommand(long flowId) {
		this.flowId = flowId;
	}

	@Override
	public boolean canExecute() {
		return target != null && source != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		var graph = source.getGraph();
		ProductSystem system = graph.getProductSystem();
		processLink = getProcessLink();
		system.processLinks.add(processLink);
		graph.linkSearch.put(processLink);
		link = new Link(processLink, source, target);
		link.getTargetNode().updateIsExpanded(INPUT);
		link.getSourceNode().updateIsExpanded(OUTPUT);
		graph.editor.setDirty();
	}

	private ProcessLink getProcessLink() {
		if (processLink == null)
			processLink = new ProcessLink();
		processLink.flowId = flowId;
		var graph = getGraph();
		if (graph == null)
			return processLink;
		FlowType type = graph.flows.type(flowId);
		if (target != null) {
			var descriptor = target.getNode().descriptor;
			if (type == FlowType.PRODUCT_FLOW) {
				processLink.processId = descriptor.id;
				processLink.exchangeId = target.exchange.id;
			} else if (type == FlowType.WASTE_FLOW) {
				processLink.providerId = descriptor.id;
				processLink.setProviderType(descriptor.type);
			}
		}
		if (source != null) {
			var descriptor = source.getNode().descriptor;
			if (type == FlowType.PRODUCT_FLOW) {
				processLink.providerId = descriptor.id;
				processLink.setProviderType(descriptor.type);
			} else if (type == FlowType.WASTE_FLOW) {
				processLink.processId = descriptor.id;
				processLink.exchangeId = source.exchange.id;
			}
		}
		return processLink;
	}

	private Graph getGraph() {
		if (target != null)
			return target.getGraph();
		if (source != null)
			return source.getGraph();
		return null;
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
		var graph = getGraph();
		if (graph == null)
			return;
		var outputNode = graph.getNode(
				link.getSourceNode().descriptor.id);
		source = outputNode.getOutput(link.processLink);
		var inProc = graph.getNode(
				link.getTargetNode().descriptor.id);
		target = inProc.getInput(link.processLink);
	}

	@Override
	public void undo() {
		var graph = getGraph();
		if (graph == null)
			return;
		ProductSystem system = graph.getProductSystem();
		link.disconnect();
		link.getTargetNode().updateIsExpanded(INPUT);
		link.getSourceNode().updateIsExpanded(OUTPUT);
		system.processLinks.remove(processLink);
		graph.linkSearch.remove(processLink);
		graph.editor.setDirty();
	}

	public void completeWith(ExchangeItem exchangeItem) {
		if (startedFromSource) {
			target = exchangeItem;
			return;
		}
		if (exchangeItem == null)
			source = null;
		else if (!exchangeItem.isConnected())
			source = exchangeItem;
	}

}
