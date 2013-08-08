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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.graphics.Color;
import org.openlca.core.model.ProcessLink;

/**
 * 
 * Internal model for a link between two exchanges. Representing a process link
 * in the graphical editor
 * 
 * @author Sebastian Greve
 * 
 */
public class ConnectionLink {

	/**
	 * line color
	 */
	public static Color COLOR = ColorConstants.gray;

	/**
	 * String for PropertyChangeEvent 'HIGHLIGHT'
	 */
	public static String HIGHLIGHT = "Highlight link";

	/**
	 * line color if connection is highlighted
	 */
	public static Color HIGHTLIGHT_COLOR = ColorConstants.blue;

	/**
	 * String for PropertyChangeEvent 'REFRESH_SOURCE_ANCHOR'
	 */
	public static String REFRESH_SOURCE_ANCHOR = "Refresh source anchor";

	/**
	 * String for PropertyChangeEvent 'REFRESH_TARGET_ANCHOR'
	 */
	public static String REFRESH_TARGET_ANCHOR = "Refresh target anchor";

	/**
	 * The figure of the link
	 */
	private IFigure figure;

	/**
	 * The process link which is representated by this ConnectionLink
	 */
	private final ProcessLink processLink;

	/**
	 * The source node of this connection
	 */
	private ExchangeNode sourceNode;

	/**
	 * Property change support
	 */
	private final PropertyChangeSupport support = new PropertyChangeSupport(
			this);

	/**
	 * The target node of this connection
	 */
	private ExchangeNode targetNode;

	/**
	 * 
	 * Constructor for a new ConnectionLink, switches source and target node if
	 * source node is input
	 * 
	 * @param sourceNode
	 *            - the dragged {@link ExchangeNode}
	 * @param targetNode
	 *            - the {@link ExchangeNode} where the sourceNode was dropped
	 * @param processLink
	 *            - the representated process link
	 */
	public ConnectionLink(final ExchangeNode sourceNode,
			final ExchangeNode targetNode, final ProcessLink processLink) {
		if (sourceNode.getExchange().isInput()) {
			this.sourceNode = targetNode;
			this.targetNode = sourceNode;
		} else {
			this.sourceNode = sourceNode;
			this.targetNode = targetNode;
		}
		this.processLink = processLink;
	}

	/**
	 * Adds a property change listener to the support
	 * 
	 * @param listener
	 *            The listener to be added
	 */
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	@Override
	public boolean equals(final Object obj) {
		// if instance of connection link
		if (obj instanceof ConnectionLink) {
			final ConnectionLink link = (ConnectionLink) obj;
			// source, target of both links not null
			if (link.getSourceNode() != null && link.getTargetNode() != null) {
				if (getSourceNode() != null && getTargetNode() != null) {
					// source and target node are the same
					if (link.getSourceNode().getExchange().getId()
							.equals(getSourceNode().getExchange().getId())
							&& link.getTargetNode()
									.getExchange()
									.getId()
									.equals(getTargetNode().getExchange()
											.getId())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Getter of the figure
	 * 
	 * @return The figure behind this link
	 */
	public IFigure getFigure() {
		return figure;
	}

	/**
	 * Getter of the representated process link
	 * 
	 * @return processLink The process link behind the connection
	 */
	public ProcessLink getProcessLink() {
		return processLink;
	}

	/**
	 * Getter of the {@link #sourceNode}
	 * 
	 * @return sourceNode The source node of the link
	 */
	public ExchangeNode getSourceNode() {
		return sourceNode;
	}

	/**
	 * Getter of the {@link #targetNode}
	 * 
	 * @return targetNode The target node of the link
	 */
	public ExchangeNode getTargetNode() {
		return targetNode;
	}

	/**
	 * links the source node with the target node in the graphical viewer. This
	 * happens by adding the link to the source and the target node
	 */
	public void link() {
		sourceNode.add(this);
		targetNode.add(this);
	}

	/**
	 * fires a property change on 'REFRESH_SOURCE_ANCHOR'
	 */
	public void refreshSourceAnchor() {
		support.firePropertyChange(REFRESH_SOURCE_ANCHOR, null, "not null");
	}

	/**
	 * fires a property change 'REFRESH_TARGET_ANCHOR'
	 */
	public void refreshTargetAnchor() {
		support.firePropertyChange(REFRESH_TARGET_ANCHOR, null, "not null");
	}

	/**
	 * Removes a property change listener from the support
	 * 
	 * @param listener
	 *            The listener to be removed
	 */
	public void removePropertyChangeListener(
			final PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	/**
	 * Setter of the figure-field
	 * 
	 * @param figure
	 *            The figure of the link
	 */
	public void setFigure(final IFigure figure) {
		this.figure = figure;
	}

	/**
	 * fires a property change 'HIGHLIGHT' with the given value as newValue
	 * 
	 * @param value
	 *            0 = unhighlight, 1 = hightlight, 2 = highlight (first element)
	 */
	public void setHighlight(final int value) {
		support.firePropertyChange(HIGHLIGHT, null, value);
	}

	/**
	 * unlinks the source node with the target node in the graphical viewer.
	 * This happens by removing the link from the source and the target node
	 */
	public void unlink() {
		sourceNode.remove(this);
		targetNode.remove(this);
	}

}
