package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class ReconnectLinkCommand extends Command {

	private ConnectionLink link;
	private ConnectionLink oldLink;
	private ProcessNode sourceNode;
	private ProcessNode targetNode;

	ReconnectLinkCommand() {

	}

	@Override
	public boolean canExecute() {
		if (sourceNode == null)
			return false;
		if (targetNode == null)
			return false;
		if (oldLink == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		ProductSystemNode systemNode = sourceNode.getParent();
		oldLink.unlink();
		systemNode.getProductSystem().getProcessLinks()
				.remove(oldLink.getProcessLink());
		systemNode.getLinkSearch().remove(oldLink.getProcessLink());
		ProcessLink processLink = new ProcessLink();
		processLink.setRecipientId(targetNode.getProcess().getId());
		processLink.setProviderId(sourceNode.getProcess().getId());
		processLink.setFlowId(oldLink.getProcessLink().getFlowId());
		systemNode.getProductSystem().getProcessLinks().add(processLink);
		systemNode.getLinkSearch().put(processLink);
		link = new ConnectionLink();
		link.setSourceNode(sourceNode);
		link.setTargetNode(targetNode);
		link.setProcessLink(processLink);
		link.link();
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		return Messages.ReconnectProcesslink;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = sourceNode.getParent();
		ProductSystem system = systemNode.getProductSystem();
		oldLink.unlink();
		system.getProcessLinks().remove(oldLink.getProcessLink());
		systemNode.getLinkSearch().remove(oldLink.getProcessLink());
		system.getProcessLinks().add(link.getProcessLink());
		systemNode.getLinkSearch().put(link.getProcessLink());
		link.link();
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = sourceNode.getParent();
		ProductSystem system = systemNode.getProductSystem();
		link.unlink();
		system.getProcessLinks().remove(link.getProcessLink());
		systemNode.getLinkSearch().remove(link.getProcessLink());
		system.getProcessLinks().add(oldLink.getProcessLink());
		systemNode.getLinkSearch().put(oldLink.getProcessLink());
		oldLink.link();
		sourceNode.getParent().getEditor().setDirty(true);
	}

	void setLink(ConnectionLink oldLink) {
		this.oldLink = oldLink;
	}

	void setSourceNode(ProcessNode sourceNode) {
		this.sourceNode = sourceNode;
	}

	void setTargetNode(ProcessNode targetNode) {
		this.targetNode = targetNode;
	}

}
