/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.analyze.sankey;

import org.eclipse.draw2d.IFigure;
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

	public static final int MAXIMIM_WIDTH = 45;

	private IFigure figure;
	private ProcessLink processLink;
	private double ratio = 1;
	private ProcessNode sourceNode;
	private ProcessNode targetNode;

	public ConnectionLink(ProcessNode sourceNode, ProcessNode targetNode,
			ProcessLink processLink, double ratio) {
		this.ratio = ratio;
		if (processLink.getProviderOutput().isInput()) {
			this.sourceNode = targetNode;
			this.targetNode = sourceNode;
		} else {
			this.sourceNode = sourceNode;
			this.targetNode = targetNode;
		}
		this.processLink = processLink;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConnectionLink) {
			ConnectionLink link = (ConnectionLink) obj;
			if (link.getProcessLink() != null && getProcessLink() != null) {
				if (link.getProcessLink().getId()
						.equals(getProcessLink().getId())) {
					return true;
				}
			}
		}
		return false;
	}

	public IFigure getFigure() {
		return figure;
	}

	public ProcessLink getProcessLink() {
		return processLink;
	}

	public double getRatio() {
		return ratio;
	}

	public ProcessNode getSourceNode() {
		return sourceNode;
	}

	public ProcessNode getTargetNode() {
		return targetNode;
	}

	public void link() {
		sourceNode.add(this);
		if (sourceNode != targetNode)
			targetNode.add(this);
	}

	public void setFigure(IFigure figure) {
		this.figure = figure;
	}

}
