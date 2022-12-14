package org.openlca.app.editors.graphical.model.commands;

import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;
import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class RemoveSupplyChainCommand extends CollapseCommand {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ArrayList<ProcessLink> providerLinks;
	private final GraphEditor editor;

	//	Product system objects
	private Set<Long> processes = new HashSet<>();
	private Set<ProcessLink> links = new HashSet<>();
	private Set<Long> isRemoving = new HashSet<>();

	public RemoveSupplyChainCommand(ArrayList<ProcessLink> links, Graph graph) {
		super(graph.getReferenceNode(), INPUT & OUTPUT);
		providerLinks = links;
		editor = graph.getEditor();
		setLabel(M.RemoveSupplyChain.toLowerCase(Locale.ROOT));
	}

	@Override
	public boolean canExecute() {
		return (!providerLinks.isEmpty() && graph != null);
	}

	@Override
	public void execute() {
//		var linkSearch = graph.linkSearch;
//		if (graph.getReferenceNode() != null) {
//			var ref = graph.getReferenceNode().descriptor.id;
//
//			for (var link : providerLinks) {
//				if (graph.flows.type(link.flowId) == FlowType.WASTE_FLOW
//						&& linkSearch.isOnlyChainingReferenceNode(
//						link.processId, OUTPUT, ref)) {
//					MsgBox.error(M.CannotRemoveSupplyChain,
//							M.WasteFlowSupplyReference);
//					return;
//				}
//				if (graph.flows.type(link.flowId) == FlowType.PRODUCT_FLOW
//						&& linkSearch.isOnlyChainingReferenceNode(
//						link.processId, INPUT, ref)) {
//					MsgBox.error(M.CannotRemoveSupplyChain,
//							M.ProductFlowSupplyReference);
//					return;
//				}
//			}
//		}

		redo();
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void redo() {
		for (var link : providerLinks)
			if (!processes.contains(link.providerId)) {
				removeEntities(link);
				// It is necessary the remove the supply chain of the nodes that are not
				// graphically connected to the reference node.
				removeGraphicalElements(link);
			}

		graph.firePropertyChange(CHILDREN_PROP, null, null);
		if (!processes.isEmpty() || !links.isEmpty()) {
			editor.setDirty();
		}
	}

	private void removeEntities(ProcessLink link) {
		// Removing the supply chain if it does not provide to any other
		// recipient.
		var root = link.processId;
		var otherLinks = graph.linkSearch
				.getProviderLinks(link.providerId)
				.stream()
				.filter(l -> l.processId != l.providerId) // Self-loop
				.filter(l -> l != link)
				.toList();
		if (otherLinks.isEmpty()) {
			var side = graph.flows.type(link.flowId) == FlowType.PRODUCT_FLOW
					? INPUT
					: OUTPUT;
			removeChain(root, link.providerId, side);
			removeProcess(link.providerId);
		}
		removeLink(link);
	}

	private void removeGraphicalElements(ProcessLink link) {
		// Removing the supply chain if it does not provide to any other
		// recipient.
		var root = graph.getNode(link.processId);
		var provider = graph.getNode(link.providerId);
		if (root == null || provider == null)
			return;
		var linkStream = provider.getAllLinks().stream()
				.map(GraphLink.class::cast)
				.filter(con -> !con.isCloseLoop())
				.filter(con -> con.processLink != link)
				.toList();
		if (linkStream.isEmpty()) {
			var side = graph.flows.type(link.flowId) == FlowType.PRODUCT_FLOW
					? INPUT
					: OUTPUT;
			collapse(root, provider, side);
			graph.removeChildQuietly(provider);
		}
		var graphLink = graph.getLink(link);
		if (graphLink != null) {
			graphLink.disconnect();
			graph.mapProcessLinkToGraphLink.remove(link);
		}
	}

	/**
	 * Remove the ProcessLink and the GraphLink.
	 */
	private void removeLink(ProcessLink link) {
		links.add(link);
		graph.getProductSystem().processLinks.remove(link);
		graph.linkSearch.remove(link);
		var graphLink = graph.getLink(link);
		if (graphLink != null) {
			graph.mapProcessLinkToGraphLink.remove(graphLink);
			graphLink.disconnect();
		}
	}

	/**
	 * Remove the Process and the Node.
	 */
	private void removeProcess(Long process) {
		processes.add(process);
		graph.getProductSystem().processes.remove(process);
		var node = graph.getNode(process);
		if (node != null)
			graph.removeChildQuietly(node);
	}

	/**
	 * Recursively remove all the input nodes connected to the given
	 * node.
	 * This method does not remove:
	 * <ul>
	 *   <li>the reference node,</li>
	 *   <li>nodes that are chained to the reference node. </li>
	 * </ul>
	 */
	private void removeChain(Long root, Long process, int side) {
		if (isRemoving.contains(process))
			return;
		isRemoving.add(process);

		var links = side == INPUT
				? graph.linkSearch.getConnectionLinks(process)
				: graph.linkSearch.getProviderLinks(process);

		for (var link : links) {
			var thisProcess = side == INPUT
					? link.processId
					: link.providerId;
			var otherProcess = side == INPUT
					? link.providerId
					: link.processId;

			if (thisProcess != process  // wrong link
					|| otherProcess == root)  // double link
				continue;

			// Checking if the reference process belongs to the supply/demand chain.
			var refNode = graph.getReferenceNode();
			if (refNode != null) {
				if (root != refNode.descriptor.id
						&& (graph.linkSearch.isChainingReference(
						otherProcess, side, refNode.descriptor.id)
						|| otherProcess == refNode.descriptor.id)) {
					// Removing the graphical nodes that do not link to the reference.
					var node = graph.getNode(otherProcess);
					if (node != null) {

					}
					continue;
				}
			}

			removeLink(link);
			removeChain(root, otherProcess, INPUT);
			removeChain(root, otherProcess, OUTPUT);

			var linkFiltered = graph.linkSearch.getLinks(otherProcess).stream()
					.filter(l -> l.processId != l.providerId)
					.toList();
			if (!linkFiltered.isEmpty())
				continue;

			removeProcess(otherProcess);
		}
		isRemoving.remove(process);
	}

}
