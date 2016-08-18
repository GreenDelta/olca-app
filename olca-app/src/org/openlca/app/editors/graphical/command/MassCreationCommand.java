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
	private List<ProcessLink> newConnections;
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

	void setNewConnections(List<ProcessLink> newConnections) {
		this.newConnections = newConnections;
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

	@Override
	public void execute() {
		for (ProcessDescriptor process : processesToCreate)
			addNode(process);
		for (ProcessLink input : newConnections)
			link(input);
		for (ProcessNode node : model.getChildren())
			if (node.getFigure().isVisible())
				oldConstraints.put(node.getFigure(), node.getFigure()
						.getBounds().getCopy());
		((GraphLayoutManager) model.getFigure().getLayoutManager()).layout(
				model.getFigure(), model.getEditor().getLayoutType());
		model.getEditor().setDirty(true);
		if (model.getEditor().getOutline() != null)
			model.getEditor().getOutline().refresh();
	}

	private void addNode(ProcessDescriptor process) {
		if (model.getProcessNode(process.getId()) != null)
			return;
		ProcessNode node = new ProcessNode(process);
		model.getProductSystem().getProcesses().add(process.getId());
		model.add(node);
		createdNodes.add(node);
	}

	private void link(ProcessLink link) {
		ProductSystem system = model.getProductSystem();
		system.getProcessLinks().add(link);
		model.getLinkSearch().put(link);
		ConnectionLink connection = createLink(link);
		connection.link();
		createdLinks.add(connection);
	}

	private ConnectionLink createLink(ProcessLink link) {
		ProcessNode sourceNode = model.getProcessNode(link.providerId);
		ProcessNode targetNode = model.getProcessNode(link.processId);
		ConnectionLink connection = new ConnectionLink();
		connection.processLink = link;
		connection.provider = sourceNode.getProviderNode(link.flowId);
		connection.exchange = targetNode.getExchangeNode(link.exchangeId);
		return connection;
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
		if (model.getEditor().getOutline() != null)
			model.getEditor().getOutline().refresh();
		model.getEditor().setDirty(true);
	}

	private void removeNode(ProcessNode node) {
		model.getProductSystem().getProcesses()
				.remove(node.getProcess().getId());
		model.remove(node);
	}

	private void unlink(ConnectionLink link) {
		ProductSystem system = model.getProductSystem();
		system.getProcessLinks().remove(link.processLink);
		model.getLinkSearch().remove(link.processLink);
		link.unlink();
	}
}
