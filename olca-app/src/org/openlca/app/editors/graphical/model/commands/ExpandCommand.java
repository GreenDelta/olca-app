package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphFactory;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;

import java.util.List;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;
import static org.openlca.app.editors.graphical.model.Node.isInput;
import static org.openlca.app.editors.graphical.model.Node.isOutput;

public class ExpandCommand extends Command {

	private final Node host;
	private final int side;
	private final GraphEditor editor;
	private final Graph graph;

	public ExpandCommand(Node host, int side) {
		this.host = host;
		this.editor = host.editor;
		this.graph = host.getGraph();
		this.side = side;
		setLabel(M.Expand);
	}

	@Override
	public boolean canExecute() {
		return !host.isExpanded(side) && (side == INPUT || side == OUTPUT);
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		long processID = host.descriptor.id;
		List<ProcessLink> links = graph.linkSearch.getLinks(processID);

		var oldLinks = side == INPUT
			? host.getAllTargetConnections()
			: host.getAllSourceConnections();
		var oldPLinks = oldLinks.stream().map(l -> l.processLink).toList();

		for (ProcessLink pLink : links) {
			FlowType type = graph.flows.type(pLink.flowId);
			if (type == null
				|| type == FlowType.ELEMENTARY_FLOW  // elementary flow cannot be linked
				|| oldPLinks.contains(pLink))  // no need to recreate
				continue;

			boolean isProvider = processID == pLink.providerId;
			long otherID = isProvider ? pLink.processId : pLink.providerId;
			Node outNode;
			Node inNode;
			if (side == INPUT && isInput(type, isProvider)) {
				inNode = host;
				outNode = getOrCreateNode(otherID);
			} else if (side == OUTPUT && isOutput(type, isProvider)) {
				outNode = host;
				inNode = getOrCreateNode(otherID);
			} else if (processID == otherID) {  // close loop
				inNode = host;
				outNode = host;
			} else {
				continue;
			}
			new Link(pLink, outNode, inNode);
			// Update the node's expanded state on the other side in case of loops.
			graph.getNode(otherID).updateIsExpanded(side == INPUT ? OUTPUT : INPUT);
		}
		host.setExpanded(side, true);
	}

	/**
	 * Create, if necessary, a node using the <code>GraphFactory</code>.
	 * @return Return the existent or the newly created <code>Node</code> for
	 * convenience.
	 */
	private Node getOrCreateNode(long id) {
		// Checking if the node already exists.
		var node = graph.getNode(id);
		if (node != null)
			return node;

		var descriptor = GraphFactory.getDescriptor(id);
		node = editor.getGraphFactory().createNode(descriptor, null);
		graph.addChild(node);
		node.updateIsExpanded(side == INPUT ? OUTPUT : INPUT);
		return node;
	}

}
