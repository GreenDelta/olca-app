/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutType;
import org.openlca.core.editors.productsystem.graphical.ProductSystemGraphEditor;
import org.openlca.core.model.ProductSystem;

/**
 * This class represents a product system as a node
 * 
 * @author Sebastian Greve
 * 
 */
public class ProductSystemNode extends Node {

	/**
	 * String for PropertyChangeEvent 'PROPERTY_LAYOUT_MANAGER'
	 */
	public static String PROPERTY_LAYOUT_MANAGER = "Layout manager";

	/**
	 * @see #setHighlightMatchingExchangeLabels(ExchangeNode, boolean)
	 */
	private ExchangeNode actualMatcher = null;

	/**
	 * The graphical editor
	 */
	private final ProductSystemGraphEditor editor;

	/**
	 * The process nodes listed in the outline
	 */
	private final List<ProcessNode> outlineNodes = new ArrayList<>();

	/**
	 * The edit part of the node
	 */
	private ProductSystemPart part;

	/**
	 * The product system this node represents
	 */
	private final ProductSystem productSystem;

	/**
	 * @see #setHighlightMatchingExchangeLabels(ExchangeNode, boolean)
	 */
	private boolean repeat = false;

	/**
	 * Constructor for a new {@link ProductSystemNode}
	 * 
	 * @param productSystem
	 *            - the product system represented by this node
	 * @param editor
	 *            - the graphical editor
	 */
	public ProductSystemNode(final ProductSystem productSystem,
			final ProductSystemGraphEditor editor) {
		this.productSystem = productSystem;
		this.editor = editor;
	}

	/**
	 * Highlights/Unhighlights the matching labels which processes are not
	 * connected yet
	 * 
	 * @param exchangeNode
	 *            The exchange node to highlight
	 * @param value
	 *            The new value (if true -> highlight, else unhighlight)
	 */
	private void setHighlightMatchingExchangeLabels(
			final ExchangeNode exchangeNode, final boolean value) {
		if (!repeat || !value) {
			repeat = true;
			if (value) {
				actualMatcher = exchangeNode;
			}
			if (actualMatcher != null) {
				for (final Node node : getChildrenArray()) {
					if (node instanceof ProcessNode) {
						for (final ExchangeNode exchangeNode2 : ((ProcessNode) node)
								.getExchangeNodes()) {
							final boolean matches = exchangeNode2
									.matches(actualMatcher);
							if (matches) {
								if (value) {
									if (actualMatcher.getExchange().isInput()
											|| !hasConnection(exchangeNode2)) {
										exchangeNode2.getFigure().setHighlight(
												value);
									}
								} else {
									exchangeNode2.getFigure().setHighlight(
											value);
								}

							}
						}
					}
				}
			}
			if (!value) {
				actualMatcher = null;
				repeat = false;
			}
		}
	}

	@Override
	public boolean addChild(final Node child) {
		if (!getChildrenArray().contains(child)) {
			if (outlineNodes.contains(child)) {
				outlineNodes.remove(child);
			}
		}
		return super.addChild(child);
	}

	/**
	 * Adds a process node to the list of outline nodes
	 * 
	 * @param node
	 *            The node to add
	 */
	public void addOutlineNode(final ProcessNode node) {
		if (!outlineNodes.contains(node)) {
			outlineNodes.add(node);
		}
	}

	@Override
	public void dispose() {
		for (final Node node : getChildrenArray()) {
			node.dispose();
		}
		getChildrenArray().clear();
	}

	/**
	 * Getter of {@link #editor}
	 * 
	 * @return the graphical editor
	 */
	public ProductSystemGraphEditor getEditor() {
		return editor;
	}

	/**
	 * Getter of the product system edit part
	 * 
	 * @return The edit part of the node
	 */
	public ProductSystemPart getPart() {
		return part;
	}

	/**
	 * Searches for the ProcessNode representing the process with the given key
	 * 
	 * @param processKey
	 *            - the key of the process for which a ProcessNode is needed
	 * @return the ProcessNode children representing the process with the given
	 *         key
	 */
	public ProcessNode getProcessNode(final String processKey) {
		for (final Node node : getChildrenArray()) {
			if (node instanceof ProcessNode) {
				if (((ProcessNode) node).getProcess().getId()
						.equals(processKey)) {
					return (ProcessNode) node;
				}
			}
		}
		return null;
	}

	/**
	 * Getter of the represented product system
	 * 
	 * @return The product system edited
	 */
	public ProductSystem getProductSystem() {
		return productSystem;
	}

	/**
	 * Indicates if the exchange node already has connections on the recipient
	 * side
	 * 
	 * @param recipient
	 *            The node to check
	 * @return True if the exchange node is already a recipient (and linked)
	 */
	public boolean hasConnection(final ExchangeNode recipient) {
		boolean hasConnection = false;
		int i = 0;
		while (!hasConnection
				&& i < getProductSystem().getProcessLinks().length) {
			if (getProductSystem().getProcessLinks()[i].getRecipientInput()
					.getId().equals(recipient.getExchange().getId())) {
				hasConnection = true;
			} else {
				i++;
			}
		}
		return hasConnection;
	}

	/**
	 * highlights all nodes which match with the exchange from the given node
	 * 
	 * @see #setHighlightMatchingExchangeLabels(ExchangeNode, boolean)
	 * 
	 * @param exchangeNode
	 *            The node to highlight
	 */
	public void highlightMatchingExchangeLabels(final ExchangeNode exchangeNode) {
		setHighlightMatchingExchangeLabels(exchangeNode, true);
	}

	/**
	 * Setter of the GraphLayoutType. Fires property change to the edit part of
	 * this node
	 * 
	 * @param type
	 *            the new GraphLayoutType
	 * @param action
	 *            the action used to set the type
	 */
	public void setGraphLayoutType(final GraphLayoutType type,
			final IAction action) {
		getSupport().firePropertyChange(PROPERTY_LAYOUT_MANAGER, action, type);
	}

	/**
	 * Setter of the product sytem edit part
	 * 
	 * @param part
	 *            The new edit part
	 */
	public void setPart(final ProductSystemPart part) {
		this.part = part;
	}

	/**
	 * Unhighlights the before highlighted labels
	 * 
	 * @see #setHighlightMatchingExchangeLabels(ExchangeNode, boolean)
	 * 
	 */
	public void unhighlightMatchingExchangeLabels() {
		setHighlightMatchingExchangeLabels(null, false);
	}

}
