package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class CreateLinkCommand extends Command {

	public final long flowId;
	public ProcessNode sourceNode;
	public ProcessNode targetNode;
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
		return flowId != 0;
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
		if (targetNode != null)
			processLink.processId = targetNode.process.getId();
		if (sourceNode != null)
			processLink.providerId = sourceNode.process.getId();
		processLink.flowId = flowId;
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
		targetNode = systemNode.getProcessNode(link.targetNode.process.getId());
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
		link.targetNode = targetNode;
		return link;
	}

}
