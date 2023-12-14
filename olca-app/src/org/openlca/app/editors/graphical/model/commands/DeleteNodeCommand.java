package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.util.Question;
import org.openlca.core.model.ProcessLink;

import java.util.ArrayList;
import java.util.Arrays;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;

public class DeleteNodeCommand extends Command {

	private final Graph graph;
	private final GraphEditor editor;
	/**
	 * Node to remove.
	 */
	private final Node node;
	private int answer;

	/**
	 * Create a command that will remove the node from its parent.
	 *
	 * @param graph the parent containing the child
	 * @param node  the component to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public DeleteNodeCommand(Graph graph, Node node) {
		this.graph = graph;
		editor = graph.getEditor();
		setLabel("delete node");
		this.node = node;
	}

	@Override
	public boolean canExecute() {
		if (node == null)
			return false;
		long refID = node.getGraph().getProductSystem().referenceProcess.id;
		return node.descriptor.id != refID;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		answer = Question.ask("Deleting " + node.descriptor.name,
				DeleteManager.QUESTION,
				Arrays.stream(DeleteManager.Answer.values()).map(Enum::name).toArray(String[]::new));

		if (answer != DeleteManager.Answer.Cancel.ordinal()) {
			redo();
		}
	}

	@Override
	public void redo() {
		var deleteManager = DeleteManager.on(graph);
		var connectedNodes = new ArrayList<Node>();

		// Remove the links to the suppliers
		var providerLinks = graph.linkSearch.getProviderLinks(node.descriptor.id);
		for (ProcessLink link : providerLinks) {
			var recipient = graph.getNode(link.processId);
			if (recipient != null) {
				connectedNodes.add(recipient);
			}
			graph.removeLink(link);
		}

		// Remove the links to the providers
		var connectionLinks = graph.linkSearch.getConnectionLinks(node.descriptor.id);
		for (ProcessLink link : connectionLinks) {
			var provider = deleteManager.link(link, answer);
			if (provider != null) {
				connectedNodes.add(provider);
			}
		}

		deleteManager.process(node.descriptor.id);
		for (Node node : connectedNodes) {
			deleteManager.nodeChains(node);
		}

		graph.firePropertyChange(CHILDREN_PROP, null, null);
		editor.setDirty();
	}

}
