package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.openlca.app.editors.graphical.GraphUtil;
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
		boolean matchInputs = !toMatch.getExchange().isInput();
		long flowId = toMatch.getExchange().getFlow().getId();
		for (ProcessNode node : getChildren()) {
			if (!node.isVisible() || node.isMinimized())
				continue;
			for (ExchangeNode en : GraphUtil.getExchangeNodes(node)) {
				if (en.isDummy() || !en.isVisible())
					continue;
				Exchange e = en.getExchange();
				if (matchInputs != e.isInput() || flowId != e.getFlow().getId())
					continue;
				if (node.isLinkedExchange(e.getId()))
					continue;
				en.setHighlighted(true);
			}
		}
	}

	public void removeHighlighting() {
		for (ProcessNode node : getChildren()) {
			if (!node.isVisible() || node.isMinimized())
				continue;
			for (ExchangeNode e : GraphUtil.getExchangeNodes(node)) {
				e.setHighlighted(false);
			}
		}
	}

	public void refreshChildren() {
		((ProductSystemPart) getEditPart()).refreshChildren();
	}

}
