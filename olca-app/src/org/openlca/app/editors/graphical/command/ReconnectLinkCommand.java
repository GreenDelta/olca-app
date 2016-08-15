package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
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
				.remove(oldLink.processLink);
		systemNode.getLinkSearch().remove(oldLink.processLink);
		ProcessLink processLink = new ProcessLink();
		processLink.processId = targetNode.getProcess().getId();
		processLink.providerId = sourceNode.getProcess().getId();
		processLink.flowId = oldLink.processLink.flowId;
		// TODO: exchangeId
		systemNode.getProductSystem().getProcessLinks().add(processLink);
		systemNode.getLinkSearch().put(processLink);
		link = new ConnectionLink();
		link.sourceNode = sourceNode;
		link.targetNode = targetNode;
		link.processLink = processLink;
		link.link();
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		return M.ReconnectProcesslink;
	}

	@Override
	public void redo() {
		ProductSystemNode systemNode = sourceNode.getParent();
		ProductSystem system = systemNode.getProductSystem();
		oldLink.unlink();
		system.getProcessLinks().remove(oldLink.processLink);
		systemNode.getLinkSearch().remove(oldLink.processLink);
		system.getProcessLinks().add(link.processLink);
		systemNode.getLinkSearch().put(link.processLink);
		link.link();
		systemNode.getEditor().setDirty(true);
	}

	@Override
	public void undo() {
		ProductSystemNode systemNode = sourceNode.getParent();
		ProductSystem system = systemNode.getProductSystem();
		link.unlink();
		system.getProcessLinks().remove(link.processLink);
		systemNode.getLinkSearch().remove(link.processLink);
		system.getProcessLinks().add(oldLink.processLink);
		systemNode.getLinkSearch().put(oldLink.processLink);
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
