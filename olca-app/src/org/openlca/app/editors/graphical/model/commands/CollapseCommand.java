package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.tools.graphics.model.Link;
import org.openlca.app.tools.graphics.model.Side;

import java.util.Objects;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;

public class CollapseCommand extends Command {

	private final Node host;
	private final Side side;
	public final Graph graph;

	public CollapseCommand(Node host, Side side) {
		this.host = host;
		this.graph = host.getGraph();
		this.side = side;
		setLabel(M.Collapse);
	}

	@Override
	public boolean canExecute() {
		return host.isExpanded(side)
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
		collapse(graph, host, host, side);
		host.setExpanded(side, false);
		host.getGraph().firePropertyChange(CHILDREN_PROP, null, null);
	}


	/**
	 * Recursively collapses all the input or output nodes connected to the given
	 * node.
	 * This method does not collapse:
	 *  - the reference node,
	 *  - nodes that are chained to the reference node.
	 */
	protected static void collapse(Graph graph, Node root, Node node, Side side) {
		if (node.isCollapsing)
			return;
		node.isCollapsing = true;

		// It is needed to copy the links otherwise we get a concurrent modification
		// exception
		var links = side == Side.INPUT
				? node.getAllTargetConnections().toArray(new Link[0])
				: node.getAllSourceConnections().toArray(new Link[0]);

		for (var l : links) {
			if (l instanceof GraphLink link) {
				var thisNode = side == Side.INPUT
						? link.getTargetNode()
						: link.getSourceNode();
				var otherNode = side == Side.INPUT
						? link.getSourceNode()
						: link.getTargetNode();

				if (!thisNode.equals(node)  // wrong link
						|| otherNode.equals(root))  // double link
					continue;

				if (!Objects.equals(root, graph.getReferenceNode())
						&& (otherNode.isChainingReferenceNode(side)
						|| otherNode.equals(graph.getReferenceNode())))
					continue;

				graph.mapProcessLinkToGraphLink.remove(link.processLink);
				link.disconnect();
				collapse(graph, root, otherNode, Side.INPUT);
				collapse(graph, root, otherNode, Side.OUTPUT);

				if (link.isCloseLoop()) { // close loop
					root.setExpanded(side == Side.INPUT
							? Side.OUTPUT
							: Side.INPUT, false);
				}

				var linkStream = otherNode.getAllLinks().stream()
						.map(GraphLink.class::cast);
				if (!linkStream.filter(con -> !con.isCloseLoop()).toList().isEmpty())
					continue;

				graph.removeChildQuietly(otherNode);
			}
		}
		node.isCollapsing = false;
	}

}
