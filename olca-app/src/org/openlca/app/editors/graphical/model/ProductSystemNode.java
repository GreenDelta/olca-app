package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
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
			if (node.isVisible() && !node.isMinimized()) {
				ExchangeNode inputNode = node.getInputNode(toMatch
						.getExchange().getFlow().getId());
				highlightExchange(node, inputNode, toMatch);
				ExchangeNode outputNode = node.getOutputNode(toMatch
						.getExchange().getFlow().getId());
				highlightExchange(node, outputNode, toMatch);
			}
		}
	}

	private void highlightExchange(ProcessNode node, ExchangeNode exchangeNode,
			ExchangeNode toMatch) {
		if (exchangeNode != null)
			if (toMatch.getExchange().isInput() != exchangeNode.getExchange()
					.isInput())
				if (toMatch.getExchange().isInput()
						|| !node.hasIncomingConnection(exchangeNode
								.getExchange().getFlow().getId()))
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
