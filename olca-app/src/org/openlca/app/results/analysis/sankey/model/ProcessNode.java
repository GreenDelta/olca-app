package org.openlca.app.results.analysis.sankey.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class ProcessNode extends Node {

	public static String CONNECTION = "Connection";

	public final CategorizedDescriptor process;
	public ProcessFigure figure;
	public final List<Link> links = new ArrayList<>();
	public double upstreamResult;
	public double upstreamContribution;
	public double directResult;
	public double directContribution;
	ProcessPart editPart;
	private Rectangle xyLayoutConstraints = new Rectangle(0, 0, 0, 0);

	public ProcessNode(CategorizedDescriptor process) {
		this.process = process;
	}

	public void add(Link link) {
		links.add(link);
		listeners.firePropertyChange(CONNECTION, null, link);
	}

	@Override
	public String getName() {
		return Labels.getDisplayName(process);
	}

	public Rectangle getXyLayoutConstraints() {
		return xyLayoutConstraints;
	}

	public void setXyLayoutConstraints(Rectangle xyLayoutConstraints) {
		this.xyLayoutConstraints = xyLayoutConstraints;
		listeners.firePropertyChange(Node.PROPERTY_LAYOUT, null, "not null");
	}

}
