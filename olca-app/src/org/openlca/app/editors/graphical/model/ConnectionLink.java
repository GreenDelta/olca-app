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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.swt.graphics.Color;
import org.openlca.core.model.ProcessLink;

public class ConnectionLink {

	public static Color COLOR = ColorConstants.gray;
	public static Color HIGHLIGHT_COLOR = ColorConstants.blue;
	public static String HIGHLIGHT = "Highlight link";
	public static String REFRESH_SOURCE_ANCHOR = "Refresh source anchor";
	public static String REFRESH_TARGET_ANCHOR = "Refresh target anchor";

	private Connection figure;
	private ProcessLink processLink;
	private ProcessNode sourceNode;
	private ProcessNode targetNode;
	private PropertyChangeSupport support = new PropertyChangeSupport(this);

	public ConnectionLink() {

	}

	public void setSourceNode(ProcessNode sourceNode) {
		this.sourceNode = sourceNode;
	}

	public void setTargetNode(ProcessNode targetNode) {
		this.targetNode = targetNode;
	}

	public void setProcessLink(ProcessLink processLink) {
		this.processLink = processLink;
	}

	public ProcessLink getProcessLink() {
		return processLink;
	}

	public Connection getFigure() {
		return figure;
	}

	void setFigure(Connection figure) {
		this.figure = figure;
	}

	public ProcessNode getSourceNode() {
		return sourceNode;
	}

	public void refreshSourceAnchor() {
		support.firePropertyChange(REFRESH_SOURCE_ANCHOR, null, "not null");
	}

	public ProcessNode getTargetNode() {
		return targetNode;
	}

	public void refreshTargetAnchor() {
		support.firePropertyChange(REFRESH_TARGET_ANCHOR, null, "not null");
	}

	/**
	 * @param value
	 *            0 = unhighlight, 1 = hightlight, 2 = highlight (first element)
	 */
	public void setHighlighted(int value) {
		support.firePropertyChange(HIGHLIGHT, null, value);
	}

	public void link() {
		sourceNode.add(this);
		targetNode.add(this);
	}

	public void unlink() {
		sourceNode.remove(this);
		targetNode.remove(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(
			final PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConnectionLink))
			return false;
		if (getSourceNode() == null)
			return false;
		if (getTargetNode() == null)
			return false;

		ConnectionLink link = (ConnectionLink) obj;
		if (link.getSourceNode() == null)
			return false;
		if (link.getTargetNode() == null)
			return false;

		if (link.getProcessLink().getFlowId() != getProcessLink().getFlowId())
			return false;
		return true;
	}

	public boolean isVisible() {
		return getFigure() != null ? getFigure().isVisible() : false;
	}

	public void setVisible(boolean value) {
		if (getFigure() != null)
			getFigure().setVisible(value);
	}

}
