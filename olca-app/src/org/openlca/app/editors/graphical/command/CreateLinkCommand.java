package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class CreateLinkCommand extends Command {

	public final long flowId;
	public ExchangeNode output;
	public ExchangeNode input;
	public boolean startedFromOutput;
	private ProcessLink processLink;
	private Link link;

	public CreateLinkCommand(long flowId) {
		this.flowId = flowId;
	}

	@Override
	public boolean canExecute() {
		return input != null && output != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		ProductSystemNode systemNode = output.parent().parent();
		ProductSystem system = systemNode.getProductSystem();
		processLink = getProcessLink();
		system.processLinks.add(processLink);
		systemNode.linkSearch.put(processLink);
		link = getLink();
		link.link();
		systemNode.editor.setDirty(true);
	}

	private ProcessLink getProcessLink() {
		if (processLink == null)
			processLink = new ProcessLink();
		processLink.flowId = flowId;
		ProductSystemNode sysNode = sysNode();
		if (sysNode == null)
			return processLink;
		FlowType type = sysNode.flows.type(flowId);
		if (input != null) {
			long processID = input.parent().process.id;
			if (type == FlowType.PRODUCT_FLOW) {
				processLink.processId = processID;
				processLink.exchangeId = input.exchange.id;
			} else if (type == FlowType.WASTE_FLOW) {
				processLink.providerId = processID;
			}
		}
		if (output != null) {
			long processID = output.parent().process.id;
			if (type == FlowType.PRODUCT_FLOW) {
				processLink.providerId = processID;
			} else if (type == FlowType.WASTE_FLOW) {
				processLink.processId = processID;
				processLink.exchangeId = output.exchange.id;
			}
		}
		return processLink;
	}

	private ProductSystemNode sysNode() {
		if (input != null)
			return input.parent().parent();
		if (output != null)
			return output.parent().parent();
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
		ProductSystemNode sys = sysNode();
		ProcessNode outProc = sys.getProcessNode(
				link.outputNode.process.id);
		output = outProc.getOutput(link.processLink);
		ProcessNode inProc = sys.getProcessNode(
				link.inputNode.process.id);
		input = inProc.getInput(link.processLink);
	}

	@Override
	public void undo() {
		ProductSystemNode sys = sysNode();
		ProductSystem system = sys.getProductSystem();
		link.unlink();
		system.processLinks.remove(processLink);
		sys.linkSearch.remove(processLink);
		sys.editor.setDirty(true);
	}

	public Link getLink() {
		if (link == null)
			link = new Link();
		link.processLink = getProcessLink();
		if (output != null)
			link.outputNode = output.parent();
		if (input != null)
			link.inputNode = input.parent();
		return link;
	}

	public void completeWith(ExchangeNode node) {
		if (startedFromOutput) {
			input = node;
			return;
		}
		if (node == null)
			output = null;
		else if (!node.parent().isConnected(node.exchange.id))
			output = node;
	}
}
