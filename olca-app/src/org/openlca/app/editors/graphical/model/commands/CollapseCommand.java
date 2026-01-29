package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiConsumer;

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

	protected static void collapse(Graph graph, Node start, Side side) {
		if (graph == null || start == null || side == null) {
			return;
		}
		var config = new Config(graph, start, side);
		new Collapse(config).run();
	}

	private record Collapse(Config config) {

		void run() {

			var handled = new HashSet<Node>();
			var removals = new HashSet<Node>();

			var queue = Task.queueOf(config);
			while (!queue.isEmpty()) {
				var task = queue.poll();
				var node = task.node;
				if (handled.contains(node)) continue;
				handled.add(node);

				task.eachLink((link, next) -> {

					config.graph.removeGraphLink(link.processLink);
					if (handled.contains(next)) return;

					boolean hasOtherLinks = next
						.getAllLinks()
						.stream()
						.filter(GraphLink.class::isInstance)
						.map(GraphLink.class::cast)
						.anyMatch(con -> !con.isSelfLoop());
					if (hasOtherLinks) return;

					removals.add(next);
					task.schedule(next, queue);
				});
			}

			removals.forEach(config.graph::removeChildQuietly);
		}
	}

	private record Config(Graph graph, Node start, Side side) {
	}

	private record Task(Config config, Node node, boolean forInputs) {

		static Queue<Task> queueOf(Config config) {
			var queue = new ArrayDeque<Task>();
			switch (config.side) {
				case INPUT -> queue.add(new Task(config, config.start, true));
				case OUTPUT -> queue.add(new Task(config, config.start, false));
				case null, default -> {
					queue.add(new Task(config, config.start, true));
					queue.add(new Task(config, config.start, false));
				}
			}
			return queue;
		}

		void schedule(Node next, Queue<Task> queue) {
			queue.add(new Task(config, next, true));
			queue.add(new Task(config, next, false));
		}

		void eachLink(BiConsumer<GraphLink, Node> fn) {
			var links = forInputs
				? node.getAllTargetConnections()
				: node.getAllSourceConnections();

			for (var l : links) {
				if (!(l instanceof GraphLink link)) continue;

				var thisNode = forInputs
					? link.getTargetNode()
					: link.getSourceNode();
				// test if the link is correct
				if (!node.equals(thisNode)) continue;

				var other = forInputs
					? link.getSourceNode()
					: link.getTargetNode();

				if (canRemoveLinkTo(other)) {
					fn.accept(link, other);
				}
			}
		}

		private boolean canRemoveLinkTo(Node other) {
			// do not remove links to the reference node
			if (config.graph.isReferenceProcess(other)) {
				return false;
			}

			// if the start node is the reference node, we can
			// collapse everything along the path
			if (config.graph.isReferenceProcess(config.start)) {
				return true;
			}

			// do not collapse a node along the path, that has
			// a connection to the reference node
			var chainSide = forInputs ? Side.INPUT : Side.OUTPUT;
			return !node.isChainingReferenceNode(chainSide);
		}
	}

}
