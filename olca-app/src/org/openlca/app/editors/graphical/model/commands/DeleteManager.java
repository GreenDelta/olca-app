package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.app.M;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.ProcessLink;
import org.openlca.util.ProviderChainRemoval;

public class DeleteManager {

	private final Graph graph;
	private final GraphEditor editor;
	public static final String QUESTION = "* " + M.DeleteLinkKeep + "\n"
			+ "* " + M.DeleteLinkHide + "\n"
			+ "* " + M.DeleteLinkDelete + "\n"
			+ "* " + M.DeleteWontDeleteProviders;

	private DeleteManager(Graph graph) {
		this.graph = graph;
		editor = graph.getEditor();
	}

	public static DeleteManager on(Graph graph) {
		return new DeleteManager(graph);
	}

	/**
	 * Remove the chain of Nodes associated with the link if it is not connected
	 * to the reference Node.
	 */
	protected void removeNodeChains(Node node) {
		if (node == null || Objects.equals(node, graph.getReferenceNode()))
			return;

		if (!node.isChainingReferenceNode(Side.INPUT)) {
			CollapseCommand.collapse(graph, node, node, Side.INPUT);
		}
		if (!node.isChainingReferenceNode(Side.OUTPUT)) {
			CollapseCommand.collapse(graph, node, node, Side.OUTPUT);
		}

		// Remove the Nodes if it is not connected to any other node.
		var links = node.getAllLinks();
		var linkStream = links.stream()
				.map(GraphLink.class::cast)
				.filter(con -> !con.isSelfLoop())
				.toList();

		if (linkStream.isEmpty()) {
			graph.removeChildQuietly(node);
		}
	}

	/**
	 * Remove the Process and the Node.
	 */
	protected void process(Long process) {
		graph.getProductSystem().processes.remove(process);
		var node = graph.getNode(process);
		if (node != null) {
			if (editor.isDirty(node.getEntity())) {
				editor.removeDirty(node.getEntity());
			}
			graph.removeChildQuietly(node);
		}
	}

	/**
	 * Deletes the process link and the graph link and remove the supply chain
	 * depending on the user's answer.
	 * Returns the provider of the link if the user chooses to delete or hide the
	 * supply chain.
	 */
	public Node link(ProcessLink link, int answer) {
		graph.removeGraphLink(link);
		var provider = graph.getNode(link.providerId);

		switch (Answer.values()[answer]) {
			case Keep -> {
				graph.removeProcessLink(link);
				return null;
			}
			case Hide -> {
				graph.removeProcessLink(link);
				return provider;
			}
			case Delete -> {
				var r = ProviderChainRemoval.on(graph.getProductSystem());
				r.remove(link);
				graph.linkSearch.rebuild(graph.getProductSystem().processLinks);
				return provider;
			}
			case Cancel -> {
				return null;
			}
		}
		return null;
	}

	public void graphLinks(List<GraphLink> graphLinks, int answer) {
		var providerNodes = new ArrayList<Node>();
		for (GraphLink link : graphLinks) {
			var provider = link(link.processLink, answer);
			if (provider != null) {
				providerNodes.add(provider);
			}
		}

		// Remove the provider and their chains if not graphically connected to the
		// reference node.
		for (Node node : providerNodes) {
			removeNodeChains(node);
		}
		graph.notifyChange(CHILDREN_PROP);
		editor.setDirty();
	}

	enum Answer {
		Cancel, Keep, Hide, Delete
	}

}
