package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
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
	 * - the reference node,
	 * - nodes that are chained to the reference node.
	 */
	protected static void collapse(Graph graph, Node root, Node node, Side side) {
		if (node.isCollapsing)
			return;
		node.isCollapsing = true;

		var links = side == Side.INPUT
			? node.getAllTargetConnections()
			: node.getAllSourceConnections();

		for (var l : links) {
			if (!(l instanceof GraphLink link)) continue;

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

			graph.removeGraphLink(link.processLink);
			collapse(graph, root, otherNode, Side.INPUT);
			collapse(graph, root, otherNode, Side.OUTPUT);

			if (link.isSelfLoop()) {
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
		node.isCollapsing = false;
	}

	private static class Collapse {

		private final Graph graph;
		private final Node start;
		private final Side side;

		Collapse(Graph graph, Node start, Side side) {
			this.graph = graph;
			this.start = start;
			this.side = side;
		}

		void run() {
			var handled = new HashSet<Node>();
			var removals = new HashSet<Node>();
			var queue = new ArrayDeque<Node>();
			queue.add(start);
			while (!queue.isEmpty()) {
				var node = queue.poll();
				handled.add(node);

				var links = side == Side.INPUT
					? node.getAllTargetConnections()  // input links
					: node.getAllSourceConnections(); // output links

				for (var l : links) {
					if (!(l instanceof GraphLink link)) continue;

					var thisNode = side == Side.INPUT
						? link.getTargetNode()  // input
						: link.getSourceNode(); // output
					if (node.equals(thisNode)) continue; // an error!

					var nextNode = side == Side.INPUT
						? link.getSourceNode()
						: link.getTargetNode();
					if (!canCollapse(nextNode)) continue;

					graph.removeGraphLink(link.processLink);

				}
			}
		}

		/// We can collapse a node along a path if it is *not* the reference node
		/// and:
		/// - the start node is the reference node
		/// - or, there is no chain the opposite collapse direction to the reference
		///   node
		private boolean canCollapse(Node node) {
			if (graph.isReferenceProcess(node)){
				return false;
			}
			return graph.isReferenceProcess(start)
				|| !node.isChainingReferenceNode(side);
		}

	}

}
