package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.*;

import java.util.Objects;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.components.graphics.model.Link;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;

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
		host.getGraph().notifyChange(CHILDREN_PROP);
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

				if (link.isSelfLoop()) { // close loop
					root.setExpanded(side == Side.INPUT
							? Side.OUTPUT
							: Side.INPUT, false);
				}

				boolean hasOtherLinks = otherNode
					.getAllLinks()
					.stream()
          .filter(GraphLink.class::isInstance)
          .map(GraphLink.class::cast)
          .anyMatch(con -> !con.isSelfLoop());
        if (hasOtherLinks) continue;

				graph.removeChildQuietly(otherNode);
			}
		}
		node.isCollapsing = false;
	}

	static class Collapse {

		private final Graph graph;
		private final Node start;
		private final Side side;

		Collapse(Graph graph, Node start, Side side) {
			this.graph = graph;
			this.start = start;
			this.side = side;
		}

		void run() {

		}

	}

}
