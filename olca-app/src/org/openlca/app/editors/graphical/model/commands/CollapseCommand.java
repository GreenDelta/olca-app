package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.*;

import java.util.ArrayDeque;
import java.util.HashSet;
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
		collapse(graph, host, side);
		host.setExpanded(side, false);
		host.getGraph().notifyChange(CHILDREN_PROP);
	}

	protected static void collapse(Graph graph, Node start, Side side) {
		if (graph == null || start == null || side == null) {
			return;
		}
		new Collapse(graph, start, side).run();
	}

	private record Collapse(Graph graph, Node start, Side side) {

		void run() {

			// walk through the graph and remove links
			var handled = new HashSet<Task>();
			var queue = Task.queueOf(side, start);
			while (!queue.isEmpty()) {
				var task = queue.poll();
				if (handled.contains(task)) continue;
				handled.add(task);
				eachLink(task, (link, next) -> {
					graph.removeGraphLink(link.processLink);
					task.schedule(next, queue);
				});
			}

			// collect the visited nodes excluding the start
			var visited = new HashSet<Node>();
			for (var task : handled) {
				if (!start.equals(task.node)) {
					visited.add(task.node);
				}
			}

			// remove visited nodes that have no remaining links
			for (var node : visited) {
				boolean hasRemainingLinks = node
					.getAllLinks()
					.stream()
					.filter(GraphLink.class::isInstance)
					.map(GraphLink.class::cast)
					.anyMatch(con -> !con.isSelfLoop());
				if (!hasRemainingLinks) {
					graph.removeChildQuietly(node);
				}
			}
		}

		void eachLink(Task task, BiConsumer<GraphLink, Node> fn) {
			var links = task.forInputs
				? task.node.getAllTargetConnections()
				: task.node.getAllSourceConnections();

			for (var l : links) {
				if (!(l instanceof GraphLink link)) continue;

				var thisNode = task.forInputs
					? link.getTargetNode()
					: link.getSourceNode();
				// test if the link is correct
				if (!task.node.equals(thisNode)) continue;

				var other = task.forInputs
					? link.getSourceNode()
					: link.getTargetNode();
				if (canRemoveLinkTo(task, other)) {
					fn.accept(link, other);
				}
			}
		}

		private boolean canRemoveLinkTo(Task task, Node other) {
			// do not remove links to the reference node
			if (graph.isReferenceProcess(other)) {
				return false;
			}

			// if the start node is the reference node, we can
			// collapse everything along the path
			if (graph.isReferenceProcess(start)) {
				return true;
			}

			// do not collapse a node along the path, that has
			// a connection to the reference node
			var chainSide = task.forInputs ? Side.INPUT : Side.OUTPUT;
			return !other.isChainingReferenceNode(chainSide);
		}
	}

	private record Task(Node node, boolean forInputs) {

		static Queue<Task> queueOf(Side side, Node start) {
			var queue = new ArrayDeque<Task>();
			switch (side) {
				case INPUT -> queue.add(new Task(start, true));
				case OUTPUT -> queue.add(new Task(start, false));
				case null, default -> {
					queue.add(new Task(start, true));
					queue.add(new Task(start, false));
				}
			}
			return queue;
		}

		void schedule(Node next, Queue<Task> queue) {
			queue.add(new Task(next, true));
			queue.add(new Task(next, false));
		}
	}

}
