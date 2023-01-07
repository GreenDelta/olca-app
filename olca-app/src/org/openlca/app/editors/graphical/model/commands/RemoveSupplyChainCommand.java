package org.openlca.app.editors.graphical.model.commands;

import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;

import java.util.ArrayList;
import java.util.Locale;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;
import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class RemoveSupplyChainCommand extends AbstractRemoveCommand {

	private final ArrayList<ProcessLink> providerLinks;

	public RemoveSupplyChainCommand(ArrayList<ProcessLink> links, Graph graph) {
		super(graph);
		providerLinks = links;
		setLabel(M.RemoveSupplyChain.toLowerCase(Locale.ROOT));
	}

	@Override
	public boolean canExecute() {
		return (!providerLinks.isEmpty() && graph != null);
	}

	@Override
	public void execute() {
		if (!checkChains())
			return;
		redo();
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void redo() {
		for (var link : providerLinks) {
			if (!processes.contains(link.providerId))
				removeEntities(link, true);
		}

		// Remove the supply chain of the nodes that are not graphically
		// connected to the reference node.
		removeNodeChains();

		graph.firePropertyChange(CHILDREN_PROP, null, null);
		if (!processes.isEmpty() || !links.isEmpty()) {
			editor.setDirty();
		}
	}

	/**
	 * Check if none of the providers is only chaining with the reference node.
	 */
	private boolean checkChains() {
		var linkSearch = graph.linkSearch;
		if (graph.getReferenceNode() != null) {
			var ref = graph.getReferenceNode().descriptor.id;

			for (var link : providerLinks) {
				if (graph.flows.type(link.flowId) == FlowType.WASTE_FLOW
						&& linkSearch.isOnlyChainingReferenceNode(
						link.processId, OUTPUT, ref)) {
					MsgBox.error(M.CannotRemoveSupplyChain,
							M.WasteFlowSupplyReference);
					return false;
				}
				if (graph.flows.type(link.flowId) == FlowType.PRODUCT_FLOW
						&& linkSearch.isOnlyChainingReferenceNode(
						link.processId, INPUT, ref)) {
					MsgBox.error(M.CannotRemoveSupplyChain,
							M.ProductFlowSupplyReference);
					return false;
				}
			}
		}
		return true;
	}

}
