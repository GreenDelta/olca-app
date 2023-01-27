package org.openlca.app.editors.graphical.model.commands;

import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;

public class DeleteNodeCommand extends AbstractRemoveCommand {
	/**
	 * Node to remove.
	 */
	private final Node node;

	/**
	 * Create a command that will remove the node from its parent.
	 *
	 * @param graph the parent containing the child
	 * @param node  the component to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public DeleteNodeCommand(Graph graph, Node node) {
		super(graph);
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
		var b = Question.ask("Remove process",
				"Remove " + Labels.name(node.descriptor)
						+ " from product system " + Labels.name(graph.getProductSystem())
						+ "?"
						+ "\nThis action will also remove the processes that are only " +
						"linked to this process."
		);
		if (!b)
			return;

		redo();
	}

	@Override
	public void redo() {
		var root = node.descriptor.id;
		var ref = graph.getReferenceNode().descriptor.id;

		// Remove the links to a process that is chained to the reference process.
		graph.linkSearch.getProviderLinks(node.descriptor.id).stream()
				.filter(link ->
						graph.linkSearch.isChainingReference(link.providerId, false, ref))
				.forEach(this::removeLink);
		graph.linkSearch.getConnectionLinks(node.descriptor.id).stream()
				.filter(link ->
						graph.linkSearch.isChainingReference(link.processId, true, ref))
				.forEach(this::removeLink);

		// Remove the supply and demand chain.
		removeChain(root, root, false);
		removeChain(root, root, true);

		// Remove eventual remaining graphical links
		node.getAllLinks().stream()
				.map(GraphLink.class::cast)
				.forEach(this::removeGraphLinkOnly);

		removeProcess(node.descriptor.id);
		removeNodeChains();

		graph.firePropertyChange(CHILDREN_PROP, null, null);
		if (!processes.isEmpty() || !links.isEmpty())
			editor.setDirty();
	}

}
