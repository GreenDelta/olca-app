package org.openlca.app.results.analysis.sankey.model;

import java.util.Objects;

import org.eclipse.draw2d.IFigure;
import org.openlca.core.model.ProcessLink;

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
		this.sourceNode = sourceNode;
		this.targetNode = targetNode;
		this.processLink = processLink;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ConnectionLink))
			return false;
		ConnectionLink other = (ConnectionLink) obj;
		return Objects.equals(this.processLink, other.processLink);
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
