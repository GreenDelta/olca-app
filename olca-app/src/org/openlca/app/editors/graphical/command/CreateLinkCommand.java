package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class CreateLinkCommand extends Command {

	public boolean startedFromSource;
	public ProcessNode sourceNode;
	public ProcessNode targetNode;
	public long flowId;

	private ConnectionLink link;
	private ProcessLink processLink;

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
		ProductSystemNode systemNode = sourceNode.getParent();
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
		if (targetNode != null)
			processLink.processId = targetNode.getProcess().getId();
		if (sourceNode != null)
			processLink.providerId = sourceNode.getProcess().getId();
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
		ProductSystemNode systemNode = sourceNode.getParent();
		sourceNode = systemNode.getProcessNode(link.sourceNode
				.getProcess().getId());
		targetNode = systemNode.getProcessNode(link.targetNode
				.getProcess().getId());
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = sourceNode.getParent();
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
		link.sourceNode = sourceNode;
		link.targetNode = targetNode;
		return link;
	}

}
