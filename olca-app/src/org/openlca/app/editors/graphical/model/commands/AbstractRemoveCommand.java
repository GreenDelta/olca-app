package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.ProcessLink;

import java.util.HashSet;
import java.util.Set;

import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

/**
 * A starting class for commands removing processes, supply and demand chains.
 * Please note that the complexity of removal commands takes its source from the
 * duality DB/Graphics: Removing a Node does not always mean removing a process.
 */
public abstract class AbstractRemoveCommand extends Command {

	protected final GraphEditor editor;
	protected final Graph graph;

	//	Product system objects
	protected final Set<Long> processes = new HashSet<>();
	protected final Set<ProcessLink> links = new HashSet<>();
	protected final Set<Long> isRemoving = new HashSet<>();
	protected final Set<Node> nodes = new HashSet<>();

	public AbstractRemoveCommand(Graph graph) {
		this.graph = graph;
		editor = graph.getEditor();
	}

	/**
	 * Recursively remove all the providers or recipients connected to the given
	 * process.
	 * This method does not remove:
	 * <ul>
	 *   <li>the reference process,</li>
	 *   <li>processes that are chained to the reference process. </li>
	 * </ul>
	 *
	 * @param root     the id of the process from which the recursion starts
	 * @param process  target process
	 * @param provider if true, the providers chain is removed; if false the
	 *                 recipients chain is removed.
	 */
	protected void removeChain(Long root, Long process, boolean provider) {
		if (isRemoving.contains(process))
			return;
		isRemoving.add(process);

		var links = provider
				? graph.linkSearch.getConnectionLinks(process)
				: graph.linkSearch.getProviderLinks(process);

		for (var link : links) {
			var thisProcess = provider
					? link.processId
					: link.providerId;
			var otherProcess = provider
					? link.providerId
					: link.processId;

			if (thisProcess != process  // wrong link
					|| otherProcess == root)  // double link
				continue;

			var node = graph.getNode(otherProcess);
			if (node != null) nodes.add(node);

			// Checking if the reference process belongs to the supply/demand chain.
			var refNode = graph.getReferenceNode();
			if (refNode != null
					&& root != refNode.descriptor.id
					&& (graph.linkSearch.isChainingReference(
					otherProcess, provider, refNode.descriptor.id)
					|| otherProcess == refNode.descriptor.id))
				continue;

			removeLink(link);
			removeChain(root, otherProcess, true);
			removeChain(root, otherProcess, false);

			var linkFiltered = graph.linkSearch.getLinks(otherProcess).stream()
					.filter(l -> l.processId != l.providerId)
					.toList();
			if (!linkFiltered.isEmpty())
				continue;

			removeProcess(otherProcess);
		}
		isRemoving.remove(process);
	}


	/**
	 * Remove the supply or the demand chain of a process. If the provider (or
	 * recipient) is providing to (receiving from) another process, the chain is
	 * kept and only the link is deleted.
	 *
	 * @param link     define a pair of two processes (by definition provider and
	 *                 process).
	 * @param provider if true, the supply chain and the provider are removed;
	 *                 if false, the demand chain and the process are removed.
	 */
	protected void removeEntities(ProcessLink link, boolean provider) {
		var links = provider
				? graph.linkSearch.getProviderLinks(link.providerId)
				: graph.linkSearch.getConnectionLinks(link.processId);
		var otherLinks = links.stream()
				.filter(l -> l.processId != l.providerId) // remove self-loop
				.filter(l -> l != link) // remove the current link
				.filter(l -> {
					var process = provider ? l.processId : l.providerId;
					var ref = graph.getProductSystem().referenceProcess.id;
					return graph.linkSearch.isChainingReference(process, !provider, ref);
				}) // remove link not chaining the reference process.
				.toList();

		if (otherLinks.isEmpty()) {
			var process = provider ? link.providerId : link.processId;
			removeChain(process, process, true);
			removeChain(process, process, false);
			removeProcess(process);
		}
		removeLink(link);
	}

	/**
	 * Remove the chain of Nodes associated with the link if it is not connected
	 * to the reference Node.
	 */
	protected void removeNodeChains() {
		for (var node : nodes) {
			if (node == graph.getReferenceNode())
				continue;

			if (!node.isChainingReferenceNode(INPUT))
				CollapseCommand.collapse(graph, node, node, INPUT);
			if (!node.isChainingReferenceNode(OUTPUT))
				CollapseCommand.collapse(graph, node, node, OUTPUT);

			var links = node.getAllLinks();
			var linkStream = links.stream()
					.map(GraphLink.class::cast)
					.filter(con -> !con.isCloseLoop())
					.toList();

			if (linkStream.isEmpty())
				graph.removeChildQuietly(node);
		}
	}

	/**
	 * Remove the ProcessLink and the GraphLink.
	 */
	protected void removeLink(ProcessLink link) {
		links.add(link);
		graph.removeLink(link);
	}

	/**
	 * Remove the Process and the Node.
	 */
	protected void removeProcess(Long process) {
		processes.add(process);
		graph.getProductSystem().processes.remove(process);
		var node = graph.getNode(process);
		if (node != null) {
			// Add the deleted node if not already done.
			nodes.add(node);
			graph.removeChildQuietly(node);
		}
	}

	protected void removeGraphLinkOnly(GraphLink graphLink) {
		if (graphLink != null) {
			graphLink.disconnect();
			graph.mapProcessLinkToGraphLink.remove(graphLink.processLink);
		}
	}

}
