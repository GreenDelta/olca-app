package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openlca.app.editors.graphical.model.GraphFactory.createGraphLink;
import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;

public class MassCreationCommand extends Command {

	private final GraphEditor editor;
	private final Graph graph;
	private final List<RootDescriptor> toCreate;
	private final List<ProcessLink> newLinks;
	// for undoing
	private final Map<Node, Rectangle> oldConstraints = new HashMap<>();
	private final List<Node> createdNodes = new ArrayList<>();
	private final List<GraphLink> createdLinks = new ArrayList<>();

	public static MassCreationCommand nextTier(List<RootDescriptor> toCreate,
			List<ProcessLink> newConnections, Graph graph) {
		return new MassCreationCommand(graph, toCreate, newConnections, M.BuildNextTier);
	}

	public static MassCreationCommand providers(List<RootDescriptor> toCreate,
			List<ProcessLink> newConnections, Graph model) {
		return new MassCreationCommand(model, toCreate, newConnections, M.ConnectProviders);
	}

	public static MassCreationCommand recipients(List<RootDescriptor> toCreate,
			List<ProcessLink> newConnections, Graph graph) {
		return new MassCreationCommand(graph, toCreate, newConnections, M.ConnectRecipients);
	}

	private MassCreationCommand(Graph graph, List<RootDescriptor> toCreate,
			List<ProcessLink> newConnections, String label) {
		this.graph = graph;
		this.editor = this.graph.getEditor();
		this.toCreate = toCreate;
		this.newLinks = newConnections;
		setLabel(label);
	}

	@Override
	public void execute() {
		for (RootDescriptor process : toCreate)
			addNode(process);
		for (ProcessLink link : newLinks) {
			graph.getProductSystem().processLinks.add(link);
			graph.linkSearch.put(link);
			createGraphLink(graph, link);
		}

		for (Node node : graph.getNodes()) {
			var bounds = new Rectangle(
					node.getLocation().getCopy(),
					node.getSize().getCopy());
			oldConstraints.put(node, bounds);
		}

		editor.setDirty();
	}

	private void addNode(RootDescriptor descriptor) {
		if (graph.getNode(descriptor.id) != null)
			return;
		Node node = graph.editor.getGraphFactory().createNode(descriptor, null);
		graph.getProductSystem().processes.add(descriptor.id);
		createdNodes.add(node);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		for (GraphLink link : createdLinks)
			unlink(link);
		for (Node node : createdNodes)
			removeNodeQuietly(node);
		graph.firePropertyChange(CHILDREN_PROP, null, null);
		for (Node node : graph.getNodes()) {
			node.setSize(oldConstraints.get(node).getSize());
			node.setLocation(oldConstraints.get(node).getLocation());
		}

		editor.setDirty();

		createdLinks.clear();
		createdNodes.clear();
		oldConstraints.clear();
	}

	private void removeNodeQuietly(Node node) {
		graph.getProductSystem().processes.remove(node.descriptor.id);
		graph.removeChildQuietly(node);
	}

	private void unlink(GraphLink link) {
		var system = graph.getProductSystem();
		system.processLinks.remove(link.processLink);
		graph.linkSearch.remove(link.processLink);
		graph.mapProcessLinkToGraphLink.remove(link);
		link.disconnect();
	}

}
