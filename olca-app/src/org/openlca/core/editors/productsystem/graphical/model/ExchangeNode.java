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

import org.openlca.core.model.Exchange;

/**
 * This class represents an exchange as a {@link Node}
 * 
 * @author Sebastian Greve
 * 
 */
public class ExchangeNode extends Node {

	/**
	 * String for the PropertyChangeEvent 'CONNECTION'
	 */
	public static String CONNECTION = "Connection";

	/**
	 * The exchange which is represented by this node
	 */
	private final Exchange exchange;

	/**
	 * The figure of this node
	 */
	private ExchangeFigure figure;

	/**
	 * List of {@link ConnectionLink}s
	 */
	private final List<ConnectionLink> links = new ArrayList<>();

	/**
	 * Constructor for a new ExchangeNode
	 * 
	 * @param exchange
	 *            the exchange which is represented by the new ExchangeNode
	 */
	public ExchangeNode(final Exchange exchange) {
		this.exchange = exchange;
	}

	/**
	 * Setter of {@link #figure}
	 * 
	 * @param figure
	 *            the ExchangeFigure of this node
	 */
	protected void setFigure(final ExchangeFigure figure) {
		this.figure = figure;
	}

	/**
	 * Adds a {@link ConnectionLink} to this node and fires propertyChange
	 * 
	 * @param connectionLink
	 *            The link to add
	 */
	public void add(final ConnectionLink connectionLink) {
		links.add(connectionLink);
		getSupport().firePropertyChange(CONNECTION, null, connectionLink);
	}

	@Override
	public void dispose() {
		links.clear();
		for (final Node node : getChildrenArray()) {
			node.dispose();
		}
		getChildrenArray().clear();
	}

	/**
	 * Getter of the represented exchange
	 * 
	 * @return the exchange represented by this node
	 */
	public Exchange getExchange() {
		return exchange;
	}

	/**
	 * Getter of {@link #figure}
	 * 
	 * @return the ExchangeFigure of this node
	 */
	@Override
	public ExchangeFigure getFigure() {
		return figure;
	}

	/**
	 * Getter of links-field
	 * 
	 * @return links The connection links of this node
	 */
	public List<ConnectionLink> getLinks() {
		return links;
	}

	/**
	 * Returns the name of the flow of this exchange
	 */
	@Override
	public String getName() {
		String name = exchange.getFlow().getName();
		if (exchange.isInput()) {
			name = "(Input) " + name;
		} else {
			name = "(Output) " + name;
		}
		return name;
	}

	/**
	 * Gets the process node to which this exchange node belongs
	 * 
	 * @return the parent process node
	 */
	public ProcessNode getParentProcessNode() {
		return (ProcessNode) getParent().getParent();
	}

	/**
	 * Indicates if the exchange of the given exchange node uses the same flow
	 * as the exchange of this node
	 * 
	 * @param exchangeNode
	 *            The exchange node containing the exchange to compare
	 * @return True if the exchanges use the same flow and on is input and the
	 *         other one output
	 */
	public boolean matches(final ExchangeNode exchangeNode) {
		boolean matches = true;
		if (!exchange.getFlow().getId()
				.equals(exchangeNode.getExchange().getFlow().getId())) {
			matches = false;
		}
		if (exchange.isInput() == exchangeNode.getExchange().isInput()) {
			matches = false;
		}
		return matches;
	}

	/**
	 * Removes the given {@link ConnectionLink} and fires propertyChange
	 * 
	 * @param connectionLink
	 *            The connection link to be removed
	 */
	public void remove(final ConnectionLink connectionLink) {
		links.remove(connectionLink);
		getSupport().firePropertyChange(CONNECTION, connectionLink, null);
	}

	/**
	 * Removes all links
	 */
	public void removeAll() {
		for (final ConnectionLink link : links) {
			if (!link.getSourceNode().getParentProcessNode().getFigure()
					.isVisible()
					|| !link.getTargetNode().getParentProcessNode().getFigure()
							.isVisible()) {
				link.unlink();
			}
		}
	}

	/**
	 * Hightlights all {@link ConnectionLink}s of this node
	 * 
	 * @param value
	 *            the new highlight value
	 */
	public void setHighlightLinks(final boolean value) {
		boolean first = true;
		for (final ConnectionLink link : getLinks()) {
			if (value) {
				if (first) {
					link.setHighlight(2);
					first = false;
				} else {
					link.setHighlight(1);
				}
			} else {
				link.setHighlight(0);
			}
		}
	}
}
