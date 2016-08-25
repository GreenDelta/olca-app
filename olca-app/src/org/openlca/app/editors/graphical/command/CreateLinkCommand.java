package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class CreateLinkCommand extends Command {

	public final long flowId;
	public ProcessNode sourceNode;
	public ExchangeNode targetNode;
	public boolean startedFromSource;
	private ProcessLink processLink;
	private Link link;

	public CreateLinkCommand(long flowId) {
		this.flowId = flowId;
	}

	@Override
	public boolean canExecute() {
		if (sourceNode == null)
			return false;
		if (targetNode == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		ProductSystemNode systemNode = sourceNode.parent();
		ProductSystem system = systemNode.getProductSystem();
		processLink = getProcessLink();
		system.getProcessLinks().add(processLink);
		systemNode.linkSearch.put(processLink);
		link = getLink();
		link.link();
		systemNode.editor.setDirty(true);
	}

	private ProcessLink getProcessLink() {
		if (processLink == null)
			processLink = new ProcessLink();
		processLink.processId = targetNode.parent().process.getId();
		processLink.exchangeId = targetNode.exchange.getId();
		processLink.flowId = targetNode.exchange.getFlow().getId();
		processLink.providerId = sourceNode.process.getId();
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
		ProductSystemNode systemNode = sourceNode.parent();
		sourceNode = systemNode.getProcessNode(link.sourceNode.process.getId());
		ProcessNode targetParentNode = systemNode.getProcessNode(link.targetNode.process.getId());
		targetNode = targetParentNode.getNode(link.processLink.exchangeId);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = sourceNode.parent();
		ProductSystem system = systemNode.getProductSystem();
		link.unlink();
		system.getProcessLinks().remove(processLink);
		systemNode.linkSearch.remove(processLink);
		systemNode.editor.setDirty(true);
	}

	public Link getLink() {
		if (link == null)
			link = new Link();
		link.processLink = processLink;
		link.sourceNode = sourceNode;
		link.targetNode = targetNode.parent();
		return link;
	}

}
