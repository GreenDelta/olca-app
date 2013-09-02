/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class ProductSystemNode extends Node {

	public static String PROPERTY_LAYOUT_MANAGER = "Layout manager";
	private ProductSystemGraphEditor editor;
	private ProductSystemPart part;
	private ProductSystem productSystem;
	private List<ProcessNode> outlineNodes = new ArrayList<>();
	private ExchangeNode actualMatcher = null;
	private boolean repeat = false;

	public ProductSystemNode(ProductSystem productSystem,
			ProductSystemGraphEditor editor) {
		this.productSystem = productSystem;
		this.editor = editor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProcessNode> getChildren() {
		return (List<ProcessNode>) super.getChildren();
	}

	public ProductSystemGraphEditor getEditor() {
		return editor;
	}

	public ProductSystemPart getPart() {
		return part;
	}

	public void setPart(ProductSystemPart part) {
		this.part = part;
	}

	public ProductSystem getProductSystem() {
		return productSystem;
	}

	public void setGraphLayoutType(GraphLayoutType type, IAction action) {
		getSupport().firePropertyChange(PROPERTY_LAYOUT_MANAGER, action, type);
	}

	private void highlightMatchingExchanges(ExchangeNode exchangeNode,
			boolean value) {
		if (!repeat || !value) {
			repeat = true;
			if (value)
				actualMatcher = exchangeNode;
			if (actualMatcher != null)
				for (ProcessNode node : getChildren())
					for (ExchangeNode exchangeNode2 : node.getExchangeNodes())
						if (exchangeNode2.matches(actualMatcher))
							if (!value)
								exchangeNode2.setHighlighted(value);
							else if (actualMatcher.getExchange().isInput()
									|| !hasConnection(exchangeNode2
											.getExchange().getFlow().getId()))
								exchangeNode2.setHighlighted(value);
			if (!value) {
				actualMatcher = null;
				repeat = false;
			}
		}
	}

	public void add(ProcessNode node) {
		if (!getChildren().contains(node))
			if (outlineNodes.contains(node))
				outlineNodes.remove(node);
		super.add(node);
	}

	public void remove(ProcessNode node) {
		super.remove(node);
	}

	public ProcessNode getProcessNode(long id) {
		for (ProcessNode node : getChildren())
			if (node.getProcess().getId() == id)
				return node;
		return null;
	}

	public void addOutlineNode(ProcessNode node) {
		if (!outlineNodes.contains(node))
			outlineNodes.add(node);
	}

	public boolean hasConnection(long flowId) {
		for (ProcessLink link : getProductSystem().getProcessLinks())
			if (link.getFlowId() == flowId)
				return true;
		return false;
	}

	public void highlightMatchingExchanges(ExchangeNode exchangeNode) {
		highlightMatchingExchanges(exchangeNode, true);
	}

	public void removeHighlighting() {
		highlightMatchingExchanges(null, false);
	}

}
