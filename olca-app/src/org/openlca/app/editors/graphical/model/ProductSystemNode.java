package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProductSystem;

public class ProductSystemNode extends Node {

	private ProductSystemGraphEditor editor;
	private MutableProcessLinkSearchMap linkSearch;

	public ProductSystemNode(ProductSystemGraphEditor editor) {
		this.linkSearch = new MutableProcessLinkSearchMap(editor
				.getSystemEditor().getModel().getProcessLinks());
		this.editor = editor;
	}

	public MutableProcessLinkSearchMap getLinkSearch() {
		return linkSearch;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProcessNode> getChildren() {
		return (List<ProcessNode>) super.getChildren();
	}

	public ProductSystemGraphEditor getEditor() {
		return editor;
	}

	public ProductSystem getProductSystem() {
		return editor.getSystemEditor().getModel();
	}

	public ProcessNode getProcessNode(long id) {
		for (ProcessNode node : getChildren())
			if (node.getProcess().getId() == id)
				return node;
		return null;
	}

	@Override
	public String getName() {
		return getProductSystem().getName();
	}

	public void highlightMatchingExchanges(ExchangeNode toMatch) {
		for (ProcessNode node : getChildren()) {
			if (!node.isVisible() || node.isMinimized())
				continue;
			ExchangeNode inputNode = node.getExchangeNode(toMatch
					.getExchange().getId());
			highlightExchange(node, inputNode, toMatch);
			ExchangeNode outputNode = node.getProviderNode(toMatch
					.getExchange().getFlow().getId());
			highlightExchange(node, outputNode, toMatch);
		}
	}

	private void highlightExchange(ProcessNode node, ExchangeNode exchangeNode,
			ExchangeNode matchNode) {
		if (node == null || exchangeNode == null || matchNode == null)
			return;
		Exchange exchange = exchangeNode.getExchange();
		Exchange match = matchNode.getExchange();
		if (exchange.isInput() == match.isInput())
			return;
		if (match.isInput() || !node.isLinkedExchange(exchange.getId()))
			exchangeNode.setHighlighted(true);
	}

	public void removeHighlighting() {
		for (ProcessNode node : getChildren())
			if (node.isVisible() && !node.isMinimized())
				for (ExchangeNode exchangeNode : node.getChildren().get(0)
						.getChildren())
					exchangeNode.setHighlighted(false);
	}

	public void refreshChildren() {
		((ProductSystemPart) getEditPart()).refreshChildren();
	}

}
