package org.openlca.app.editors.graphical.model.commands;

import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.ProcessLink;
import org.openlca.util.ProviderChainRemoval;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;
import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class DeleteManager {

	private final Graph graph;
	private final GraphEditor editor;
	public static final String QUESTION =
			"- Keep: keep the supply chain even if it is not connected to " +
					"the reference process." +
					"\n- Hide: hide the supply chain but keep the processes as part " +
					"of the product system," +
					"\n- Delete: delete the supply chain (processes) from the " +
					"product system," +
					"\nPlease note that clicking delete won't delete the providers " +
					"that supply the reference process, but only hide them.";

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
	protected void nodeChains(Node node) {
		if (node == null || Objects.equals(node, graph.getReferenceNode()))
			return;

		if (!node.isChainingReferenceNode(INPUT)) {
			CollapseCommand.collapse(graph, node, node, INPUT);
		}
		if (!node.isChainingReferenceNode(OUTPUT)) {
			CollapseCommand.collapse(graph, node, node, OUTPUT);
		}

		var links = node.getAllLinks();
		var linkStream = links.stream()
				.map(GraphLink.class::cast)
				.filter(con -> !con.isCloseLoop())
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
	 * depending on user's answer.
	 * Returns the provider of the link if the user shows to delete or hide the
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
				graph.linkSearch.remove(link);
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
			nodeChains(node);
		}

		graph.firePropertyChange(CHILDREN_PROP, null, null);
		editor.setDirty();
	}

	enum Answer {
		Cancel, Keep, Hide, Delete
	}

}
