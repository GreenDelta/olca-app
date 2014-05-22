package org.openlca.app.editors.graphical.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class MassCreationCommand extends Command {

	private List<ProcessDescriptor> processesToCreate;
	private List<ConnectionInput> newConnections;
	private ProductSystemNode model;
	// for undoing
	private Map<IFigure, Rectangle> oldConstraints = new HashMap<>();
	private List<ProcessNode> createdNodes = new ArrayList<>();
	private List<ConnectionLink> createdLinks = new ArrayList<>();

	MassCreationCommand() {
	}

	void setProcessesToCreate(List<ProcessDescriptor> processesToCreate) {
		this.processesToCreate = processesToCreate;
	}

	void setNewConnections(List<ConnectionInput> newConnections) {
		this.newConnections = newConnections;
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

	@Override
	public void execute() {
		for (ProcessDescriptor process : processesToCreate)
			addNode(process);
		for (ConnectionInput input : newConnections)
			link(input.sourceId, input.targetId, input.flowId);
		for (ProcessNode node : model.getChildren())
			if (node.getFigure().isVisible())
				oldConstraints.put(node.getFigure(), node.getFigure()
						.getBounds().getCopy());
		((GraphLayoutManager) model.getFigure().getLayoutManager()).layout(
				model.getFigure(), model.getEditor().getLayoutType());
		model.getEditor().setDirty(true);
	}

	private void addNode(ProcessDescriptor process) {
		ProcessNode node = new ProcessNode(process);
		model.getProductSystem().getProcesses().add(process.getId());
		model.add(node);
		createdNodes.add(node);
	}

	private void link(long sourceId, long targetId, long flowId) {
		ProductSystem system = model.getProductSystem();
		ProcessLink processLink = createProcessLink(sourceId, targetId, flowId);
		system.getProcessLinks().add(processLink);
		model.getLinkSearch().put(processLink);
		ConnectionLink link = createLink(sourceId, targetId, processLink);
		link.link();
		createdLinks.add(link);
	}

	private ProcessLink createProcessLink(long sourceId, long targetId,
			long flowId) {
		ProcessLink processLink = new ProcessLink();
		processLink.setRecipientId(targetId);
		processLink.setProviderId(sourceId);
		processLink.setFlowId(flowId);
		return processLink;
	}

	private ConnectionLink createLink(long sourceId, long targetId,
			ProcessLink processLink) {
		ProcessNode sourceNode = model.getProcessNode(sourceId);
		ProcessNode targetNode = model.getProcessNode(targetId);
		ConnectionLink link = new ConnectionLink();
		link.setProcessLink(processLink);
		link.setSourceNode(sourceNode);
		link.setTargetNode(targetNode);
		return link;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		for (ConnectionLink link : createdLinks)
			unlink(link);
		for (ProcessNode node : createdNodes)
			removeNode(node);
		for (ProcessNode node : model.getChildren())
			if (oldConstraints.get(node.getFigure()) != null)
				node.setXyLayoutConstraints(oldConstraints.get(node.getFigure()));
		createdLinks.clear();
		createdNodes.clear();
		oldConstraints.clear();
		model.getEditor().setDirty(true);
	}

	private void removeNode(ProcessNode node) {
		model.getProductSystem().getProcesses()
				.remove(node.getProcess().getId());
		model.remove(node);
	}

	private void unlink(ConnectionLink link) {
		ProductSystem system = model.getProductSystem();
		system.getProcessLinks().remove(link.getProcessLink());
		model.getLinkSearch().remove(link.getProcessLink());
		link.unlink();
	}
}
