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

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;

/**
 * This class represents a process as a {@link Node}
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessNode extends Node {

	/**
	 * String for PropertyChangeEvent 'RESIZE_FIGURE'
	 */
	public static String RESIZE_FIGURE = "Resize figure";

	/**
	 * The figure which belongs to this node
	 */
	private ProcessFigure figure = null;

	/**
	 * Attribute for minimize/maximize status
	 */
	private boolean minimized = true;

	/**
	 * The process descriptor this node is representing
	 */
	private final Process process;

	/**
	 * The layout constraints for the XYLayoutManager
	 */
	private Rectangle xyLayoutConstraints;

	/**
	 * Constructor of a new ProcessNode. Loads the exchanges of the given
	 * process descriptor from the database and adds an
	 * {@link ExchangeContainerNode} with the loaded exchanges
	 * 
	 * @param process
	 *            The process represented by the node
	 * @param minimized
	 *            Indicates if the process should be minimized or maximized
	 */
	public ProcessNode(final Process process, final boolean minimized) {
		super();
		addChild(new ExchangeContainerNode(process.getExchanges()));
		this.minimized = minimized;
		this.process = process;
	}

	/**
	 * Setter of {@link #figure}
	 * 
	 * @param figure
	 *            The figure representing the node in the diagram
	 */
	protected void setFigure(final ProcessFigure figure) {
		xyLayoutConstraints = new Rectangle(0, 0, figure.getPreferredSize(-1,
				-1).width, figure.getPreferredSize(-1, -1).height);
		this.figure = figure;
	}

	@Override
	public void dispose() {
		for (final Node node : getChildrenArray()) {
			node.dispose();
		}
		getChildrenArray().clear();
	}

	@Override
	public boolean equals(final Object arg0) {
		boolean equals = false;
		if (arg0 instanceof ProcessNode) {
			if (((ProcessNode) arg0).getProcess().getId()
					.equals(process.getId())) {
				equals = true;
			}
		}
		return equals;
	}

	/**
	 * Returns the {@link ExchangeNode} belonging to the given exchange if it
	 * contains it, else null
	 * 
	 * @param id
	 *            The id of the exchange of the requested exchange node
	 * @return The {@link ExchangeNode} belonging to the given exchange if it
	 *         contains it, else null
	 */
	public ExchangeNode getExchangeNode(final String id) {
		ExchangeNode node = null;
		int i = 0;
		while (i < getExchangeNodes().length && node == null) {
			final ExchangeNode eNode = getExchangeNodes()[i];
			if (eNode.getExchange().getId().equals(id)) {
				node = eNode;
			} else {
				i++;
			}
		}
		return node;
	}

	/**
	 * Getter of the exchanges of the represented process
	 * 
	 * @return The exchanges of the process behind the node
	 */
	public Exchange[] getExchanges() {
		return process.getExchanges();
	}

	/**
	 * Gets the nodes which belong to the exchanges
	 * 
	 * @return The exchange node children of the process node
	 */
	public ExchangeNode[] getExchangeNodes() {
		final List<ExchangeNode> exchangesNodes = new ArrayList<>();
		for (final Node node : getChildrenArray()) {
			for (final Node node2 : node.getChildrenArray()) {
				if (node2 instanceof ExchangeNode) {
					exchangesNodes.add((ExchangeNode) node2);
				}
			}
		}
		final ExchangeNode[] result = new ExchangeNode[exchangesNodes.size()];
		exchangesNodes.toArray(result);
		return result;
	}

	/**
	 * Getter of {@link #figure}
	 * 
	 * @return figure
	 */
	@Override
	public ProcessFigure getFigure() {
		return figure;
	}

	@Override
	public String getName() {
		String text = process.getName();
		text += process.getLocation() != null ? " ["
				+ process.getLocation().getName() + "]" : "";
		return text;
	}

	/**
	 * Getter of the process descriptor
	 * 
	 * @return The process behind the node
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * Getter of the xySize of the figure
	 * 
	 * @return the size of the figure
	 */
	public Dimension getSize() {
		return getFigure().getSize();
	}

	/**
	 * Getter of xyLayoutConstraints-field
	 * 
	 * @return the x and y value of the layout
	 */
	public Rectangle getXyLayoutConstraints() {
		return xyLayoutConstraints;
	}

	/**
	 * Getter of minimized-field
	 * 
	 * @return true if process figure is minimized, else false
	 */
	public boolean isMinimized() {
		return minimized;
	}

	/**
	 * Calls {@link ConnectionLink#setHighlight(int)} of each
	 * {@link ConnectionLink} of each containing {@link ExchangeNode}
	 * 
	 * @param value
	 *            The new value
	 */
	public void setHighlightLinks(final boolean value) {
		for (final ExchangeNode exchangeNode : getExchangeNodes()) {
			for (final ConnectionLink link : exchangeNode.getLinks()) {
				if (value) {
					link.setHighlight(1);
				} else {
					link.setHighlight(0);
				}
			}
		}
	}

	/**
	 * Refreshes the xyLayoutConstraints with the actual location and preferred
	 * size and fires 'RESIZE_FIGURE' PropertyChangeEvenet
	 * 
	 * @param minimized
	 *            true if the process should be minized, false if he should set
	 *            maximized
	 */
	public void setMinimized(final boolean minimized) {
		final boolean old = this.minimized;
		this.minimized = minimized;
		xyLayoutConstraints = new Rectangle(getFigure().getLocation(),
				getFigure().getPreferredSize());
		getSupport().firePropertyChange(RESIZE_FIGURE, old, minimized);
	}

	/**
	 * Setter of xyLayoutConstraints-field
	 * 
	 * @param xyLayoutConstraints
	 *            new layout constraints
	 */
	public void setXyLayoutConstraints(final Rectangle xyLayoutConstraints) {
		this.xyLayoutConstraints = xyLayoutConstraints;
		getSupport().firePropertyChange(Node.PROPERTY_LAYOUT, null, "not null");
	}

	public boolean hasConnections() {
		boolean hasLink = false;
		for (ExchangeNode eNode : getExchangeNodes()) {
			if (eNode.getLinks().size() > 0) {
				hasLink = true;
				break;
			}
		}
		return hasLink;
	}
}
