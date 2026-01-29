package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.*;
import static org.openlca.app.editors.graphical.model.GraphFactory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.RootDescriptor;

public class MassCreationCommand extends Command {

	private final GraphEditor editor;
	private final Graph graph;
	private final List<RootDescriptor> toCreate;
	private final List<ProcessLink> newLinks;
	// for undoing
	private final Map<Node, Rectangle> oldConstraints = new HashMap<>();
	private final List<Node> createdNodes = new ArrayList<>();

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
		for (RootDescriptor process : toCreate) {
			addNode(process);
		}
		for (ProcessLink link : newLinks) {
			addLink(link);
		}

		for (Node node : graph.getNodes()) {
			var bounds = new Rectangle(
					node.getLocation().getCopy(),
					node.getSize().getCopy());
			oldConstraints.put(node, bounds);
		}

		editor.setDirty();
	}

	private void addLink(ProcessLink link) {
		var system = graph.getProductSystem();
		system.processLinks.add(link);
		graph.linkSearch.rebuild(system.processLinks);

		var graphLink = graph.getLink(link);
		if (graphLink != null) {
			graphLink.reconnect();
			return;
		}

		createGraphLink(graph, link);
	}

	private void addNode(RootDescriptor descriptor) {
		if (graph.getNode(descriptor.id) != null)
			return;
		var node = graph.editor.getGraphFactory().createNode(descriptor, null);
		graph.getProductSystem().processes.add(descriptor.id);
		graph.addChild(node);
		createdNodes.add(node);
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		for (Node node : createdNodes)
			removeNodeQuietly(node);
		graph.notifyChange(CHILDREN_PROP);
		for (Node node : graph.getNodes()) {
			node.setSize(oldConstraints.get(node).getSize());
			node.setLocation(oldConstraints.get(node).getLocation());
		}

		editor.setDirty();
		createdNodes.clear();
		oldConstraints.clear();
	}

	private void removeNodeQuietly(Node node) {
		graph.getProductSystem().processes.remove(node.descriptor.id);
		if (editor.isDirty(node.getEntity())) {
			editor.removeDirty(node.getEntity());
		}
		graph.removeChildQuietly(node);
	}

}
