package org.openlca.app.editors.graphical.model.commands;

import static org.openlca.app.components.graphics.model.Component.*;

import java.util.ArrayDeque;
import java.util.HashSet;

import org.eclipse.gef.commands.Command;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.commons.Strings;
import org.openlca.core.model.ModelType;

public class RemoveChainCommand extends Command {

	private final Graph graph;
	private final Node root;

	public RemoveChainCommand(Graph graph, Node node) {
		this.graph = graph;
		this.root = node;
		setLabel(M.RemoveSupplyChain);
	}

	@Override
	public boolean canExecute() {
		if (graph == null)
			return false;
		if (root != null) {
			return root.descriptor != null
				&& root.descriptor.type == ModelType.PROCESS
				&& !root.isReferenceProcess();
		}
		return false;
	}

	@Override
	public void execute() {
		var title = "Remove process chain - "
			+ Strings.cutEnd(Labels.name(root.descriptor), 50);
		var message = "This will remove the process and its entire process " +
			"chain from the product system, including all upstream product " +
			"providers and downstream waste treatments that are not used by " +
			"other processes in the system.";
		if (Question.ask(title, message)) {
			redo();
		}
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void redo() {
		var system = graph.getProductSystem();

		App.runInUI("Removing process chain", () -> {
			var tree = RemovalTree.of(system.processLinks, root.descriptor.id);
			system.processes.removeAll(tree.providers());
			system.processLinks.removeAll(tree.links());
			graph.linkSearch.rebuild(system.processLinks);

			for (var link : tree.links()) {
				graph.removeVisualLink(link);
			}
			for (long id : tree.providers()) {
				var node = graph.getNode(id);
				if (node != null) {
					graph.removeChildQuietly(node);
				}
			}

			removeDisconnectedNodes();

			graph.notifyChange(CHILDREN_PROP);
			graph.getEditor().setDirty();
		});
	}

	/// Removes visual nodes from the graph that are no longer visually
	/// connected to the reference process.
	private void removeDisconnectedNodes() {
		var ref = graph.getReferenceNode();
		if (ref == null)
			return;

		// collect all reachable nodes
		var reachable = new HashSet<Node>();
		var queue = new ArrayDeque<Node>();
		queue.add(ref);
		reachable.add(ref);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			for (var l : next.getAllLinks()) {
				if (!(l instanceof GraphLink link)) continue;
				var source = link.getSourceNode();
				if (source != null && reachable.add(source)) {
					queue.add(source);
				}
				var target = link.getTargetNode();
				if (target != null && reachable.add(target)) {
					queue.add(target);
				}
			}
		}

		// remove and unlink nodes that are not reachable
		var removals = new HashSet<Node>();
		for (var node : graph.getNodes()) {
			if (!reachable.contains(node)) {
				removals.add(node);
			}
		}

		for (var node : removals) {
			for (var l : node.getAllLinks()) {
				if (!(l instanceof GraphLink link)) continue;
				graph.removeVisualLink(link.processLink);
			}
			graph.removeChildQuietly(node);
		}
	}
}
