package org.openlca.app.editors.graphical.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class MassCreationCommand extends Command {

	private final ProductSystemNode model;
	private final List<ProcessDescriptor> toCreate;
	private final List<ConnectionInput> newConnections;
	// for undoing
	private final Map<IFigure, Rectangle> oldConstraints = new HashMap<>();
	private final List<ProcessNode> createdNodes = new ArrayList<>();
	private final List<Link> createdLinks = new ArrayList<>();

	public static MassCreationCommand nextTier(List<ProcessDescriptor> toCreate,
			List<ConnectionInput> newConnections, ProductSystemNode model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.BuildNextTier);
	}

	public static MassCreationCommand providers(List<ProcessDescriptor> toCreate,
			List<ConnectionInput> newConnections, ProductSystemNode model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.ConnectProviders);
	}

	public static MassCreationCommand recipients(List<ProcessDescriptor> toCreate,
			List<ConnectionInput> newConnections, ProductSystemNode model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.ConnectRecipients);
	}

	private MassCreationCommand(ProductSystemNode model, List<ProcessDescriptor> toCreate,
			List<ConnectionInput> newConnections, String label) {
		this.model = model;
		this.toCreate = toCreate;
		this.newConnections = newConnections;
		setLabel(label);
	}

	@Override
	public void execute() {
		for (ProcessDescriptor process : toCreate)
			addNode(process);
		for (ConnectionInput input : newConnections)
			link(input);
		for (ProcessNode node : model.getChildren())
			if (node.figure.isVisible())
				oldConstraints.put(node.figure, node.figure.getBounds().getCopy());
		((LayoutManager) model.figure.getLayoutManager()).layout(model.figure, model.editor.getLayoutType());
		model.editor.setDirty(true);
		if (model.editor.getOutline() != null)
			model.editor.getOutline().refresh();
	}

	private void addNode(ProcessDescriptor process) {
		if (model.getProcessNode(process.id) != null)
			return;
		ProcessNode node = new ProcessNode(process);
		model.getProductSystem().processes.add(process.id);
		model.add(node);
		createdNodes.add(node);
	}

	private void link(ConnectionInput input) {
		ProductSystem system = model.getProductSystem();
		ProcessLink processLink = createProcessLink(input);
		system.processLinks.add(processLink);
		model.linkSearch.put(processLink);
		long sourceId = input.isWaste ? input.targetId : input.sourceId;
		long targetId = input.isWaste ? input.sourceId : input.targetId;
		Link link = createLink(sourceId, targetId, processLink);
		link.link();
		createdLinks.add(link);
	}

	private ProcessLink createProcessLink(ConnectionInput input) {
		ProcessLink processLink = new ProcessLink();
		processLink.processId = input.targetId;
		processLink.providerId = input.sourceId;
		processLink.flowId = input.flowId;
		processLink.exchangeId = input.exchangeId;
		return processLink;
	}

	private Link createLink(long sourceId, long targetId, ProcessLink processLink) {
		ProcessNode sourceNode = model.getProcessNode(sourceId);
		ProcessNode targetNode = model.getProcessNode(targetId);
		Link link = new Link();
		link.processLink = processLink;
		link.outputNode = sourceNode;
		link.inputNode = targetNode;
		return link;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		for (Link link : createdLinks)
			unlink(link);
		for (ProcessNode node : createdNodes)
			removeNode(node);
		for (ProcessNode node : model.getChildren())
			if (oldConstraints.get(node.figure) != null)
				node.setXyLayoutConstraints(oldConstraints.get(node.figure));
		createdLinks.clear();
		createdNodes.clear();
		oldConstraints.clear();
		if (model.editor.getOutline() != null)
			model.editor.getOutline().refresh();
		model.editor.setDirty(true);
	}

	private void removeNode(ProcessNode node) {
		model.getProductSystem().processes.remove(node.process.id);
		model.remove(node);
	}

	private void unlink(Link link) {
		ProductSystem system = model.getProductSystem();
		system.processLinks.remove(link.processLink);
		model.linkSearch.remove(link.processLink);
		link.unlink();
	}
}
