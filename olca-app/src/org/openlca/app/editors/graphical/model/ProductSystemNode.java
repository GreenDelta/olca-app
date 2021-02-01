package org.openlca.app.editors.graphical.model;

import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.core.matrix.cache.FlowTable;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProductSystem;

public class ProductSystemNode extends Node {

	public final MutableProcessLinkSearchMap linkSearch;
	public final FlowTable flows = FlowTable.create(Database.get());
	private final TLongHashSet wasteProcesses;

	public ProductSystemNode(GraphEditor editor) {
		super(editor);
		var system = editor.getProductSystem();
		this.linkSearch = new MutableProcessLinkSearchMap(system.processLinks);
		var refProcess = system.referenceProcess;
		if (refProcess != null) {
			var refNode = ProcessNode.create(editor, refProcess.id);
			if (refNode != null) {
				add(refNode);
			}
		}
		wasteProcesses = new TLongHashSet();
		for (var link : system.processLinks) {
			var flowType = flows.type(link.flowId);
			if (flowType == FlowType.WASTE_FLOW) {
				wasteProcesses.add(link.providerId);
			}
		}
	}

	public boolean isWasteProcess(ProcessNode node) {
		return node != null
					 && node.process != null
					 && wasteProcesses.contains(node.process.id);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProcessNode> getChildren() {
		return (List<ProcessNode>) super.getChildren();
	}

	public ProductSystem getProductSystem() {
		return editor.getProductSystem();
	}

	public ProcessNode getProcessNode(long id) {
		for (ProcessNode node : getChildren())
			if (node.process.id == id)
				return node;
		return null;
	}

	@Override
	public String getName() {
		return getProductSystem().name;
	}

	public void highlightMatchingExchanges(ExchangeNode toMatch) {
		if (toMatch == null)
			return;
		for (ProcessNode node : getChildren()) {
			if (!node.isVisible() || node.isMinimized())
				continue;
			for (ExchangeNode e : node.getExchangeNodes()) {
				if (toMatch.matches(e)) {
					e.setHighlighted(true);
				}
			}
		}
	}

	public void removeHighlighting() {
		for (ProcessNode node : getChildren()) {
			if (!node.isVisible() || node.isMinimized())
				continue;
			for (ExchangeNode exchangeNode : node.getChildren().get(0).getChildren())
				exchangeNode.setHighlighted(false);
		}
	}

	public void refreshChildren() {
		editPart.refreshChildren();
	}

}
