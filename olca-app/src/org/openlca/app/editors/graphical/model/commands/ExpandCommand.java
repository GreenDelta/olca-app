package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphFactory;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;

import java.util.List;

import static org.openlca.app.editors.graphical.model.Node.isInput;
import static org.openlca.app.editors.graphical.model.Node.isOutput;
import static org.openlca.app.components.graphics.model.Component.CHILDREN_PROP;

public class ExpandCommand extends Command {

	private final Node host;
	private final Side side;
	private final GraphEditor editor;
	private final Graph graph;
	private final boolean quiet;

	public ExpandCommand(Node host, Side side, boolean quiet) {
		this.host = host;
		this.editor = host.getGraph().getEditor();
		this.graph = host.getGraph();
		this.side = side;
		this.quiet = quiet;
		setLabel(M.Expand);
	}

	@Override
	public boolean canExecute() {
		return !host.isExpanded(side)
				&& (side != Side.BOTH);
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

		var oldLinks = side == Side.INPUT
			? host.getAllTargetConnections()
			: host.getAllSourceConnections();
		var oldPLinks = oldLinks.stream()
				.map(GraphLink.class::cast)
				.map(l -> l.processLink)
				.toList();

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
			if (side == Side.INPUT && isInput(type, isProvider)) {
				inNode = host;
				outNode = getOrCreateNode(otherID);
			} else if (side == Side.OUTPUT && isOutput(type, isProvider)) {
				outNode = host;
				inNode = getOrCreateNode(otherID);
			} else if (processID == otherID) {  // close loop
				inNode = host;
				outNode = host;
			} else continue;

			var link = new GraphLink(pLink, outNode, inNode);
			graph.mapProcessLinkToGraphLink.put(pLink, link);
		}
		host.setExpanded(side, true);

		// Fire a property change if the Nodes have been added quietly.
		if (!quiet)
			graph.firePropertyChange(CHILDREN_PROP, null, null);
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
		var newNode = editor.getGraphFactory().createNode(descriptor, null);
		if (quiet) graph.addChildQuietly(newNode);
		else graph.addChild(newNode);
		newNode.updateIsExpanded(side.opposite());
		return newNode;
	}

}
