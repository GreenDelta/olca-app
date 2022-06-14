package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MassCreationCommand extends Command {

	private final Graph graph;
	private final List<RootDescriptor> toCreate;
	private final List<ProcessLink> newLinks;
	// for undoing
	private final Map<Node, Rectangle> oldConstraints = new HashMap<>();
	private final List<Node> createdNodes = new ArrayList<>();
	private final List<Link> createdLinks = new ArrayList<>();

	public static MassCreationCommand nextTier(List<RootDescriptor> toCreate,
			List<ProcessLink> newConnections, Graph model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.BuildNextTier);
	}

	public static MassCreationCommand providers(List<RootDescriptor> toCreate,
			List<ProcessLink> newConnections, Graph model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.ConnectProviders);
	}

	public static MassCreationCommand recipients(List<RootDescriptor> toCreate,
			List<ProcessLink> newConnections, Graph model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.ConnectRecipients);
	}

	private MassCreationCommand(
			Graph model,
			List<RootDescriptor> toCreate,
			List<ProcessLink> newConnections,
			String label) {
		this.graph = model;
		this.toCreate = toCreate;
		this.newLinks = newConnections;
		setLabel(label);
	}

	@Override
	public void execute() {
		for (RootDescriptor process : toCreate)
			addNode(process);
		for (ProcessLink newLink : newLinks)
			link(newLink);

		for (Node node : graph.getChildren()) {
			var bounds = new Rectangle(node.getLocation().getCopy(), node.getSize().getCopy());
			oldConstraints.put(node, bounds);
		}

		// TODO (francois) layout
		graph.editor.setDirty();
	}

	private void addNode(RootDescriptor descriptor) {
		if (graph.getNode(descriptor.id) != null)
			return;
		Node node = graph.editor.getGraphFactory().createNode(descriptor, null);
		graph.getProductSystem().processes.add(descriptor.id);
		graph.addChild(node);
		createdNodes.add(node);
	}

	private void link(ProcessLink newLink) {
		ProductSystem system = graph.getProductSystem();
		system.processLinks.add(newLink);
		graph.linkSearch.put(newLink);

		FlowType ftype = graph.flows.type(newLink.flowId);
		boolean isWaste = ftype == FlowType.WASTE_FLOW;
		var outNode = graph.getNode(
				isWaste ? newLink.processId
						: newLink.providerId);
		var inNode = graph.getNode(
				isWaste ? newLink.providerId
						: newLink.processId);
		var link = new Link(newLink, outNode, inNode);

		createdLinks.add(link);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		for (Link link : createdLinks)
			unlink(link);
		for (Node node : createdNodes)
			removeNode(node);
		for (Node node : graph.getChildren()) {
			node.setSize(oldConstraints.get(node).getSize());
			node.setLocation(oldConstraints.get(node).getLocation());
		}

		createdLinks.clear();
		createdNodes.clear();
		oldConstraints.clear();

		graph.editor.setDirty();
	}

	private void removeNode(Node node) {
		graph.getProductSystem().processes.remove(node.descriptor.id);
		graph.removeChild(node);
	}

	private void unlink(Link link) {
		ProductSystem system = graph.getProductSystem();
		system.processLinks.remove(link.processLink);
		graph.linkSearch.remove(link.processLink);
		link.disconnect();
	}
}
