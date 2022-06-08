package org.openlca.app.editors.graphical_legacy.model;

import java.util.List;
import java.util.Objects;

import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.search.MutableProcessLinkSearchMap;
import org.openlca.core.matrix.cache.FlowTable;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;

import gnu.trove.set.hash.TLongHashSet;

/**
 * A {@link ProductSystemNode} contains a system of unit processes, library
 * processes, results and/or product systems (represented by a
 * {@link ProcessNode}).
 */
public class ProductSystemNode extends Node {

	public final MutableProcessLinkSearchMap linkSearch;
	public final FlowTable flows = FlowTable.create(Database.get());
	private final TLongHashSet wasteProcesses;
	private final Process referenceProcess;

	public ProductSystemNode(GraphEditor editor) {
		super(editor);
		var system = editor.getProductSystem();
		this.linkSearch = new MutableProcessLinkSearchMap(system.processLinks);
		referenceProcess = system.referenceProcess;
		if (referenceProcess != null) {
			var refNode = ProcessNode.create(editor, referenceProcess.id);
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

	public boolean isReferenceProcess(ProcessNode node) {
		return node != null && referenceProcess.id == node.process.id;
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
		for (var node : getChildren()) {
			if (node.process != null && node.process.id == id)
				return node;
		}
		return null;
	}

	public ProcessNode getProcessNode(String refId) {
		for (var node : getChildren()) {
			if (node.process != null && Objects.equals(refId, node.process.refId))
				return node;
		}
		return null;
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
}
