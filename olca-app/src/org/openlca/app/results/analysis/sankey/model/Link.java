package org.openlca.app.results.analysis.sankey.model;

import java.util.Objects;

import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPart;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.openlca.app.FaviColor;
import org.openlca.app.util.Colors;
import org.openlca.core.model.ProcessLink;

public class Link {

	public static final int MAXIMIM_WIDTH = 45;
	public static final Color HIGHLIGHT_COLOR = Colors.get(255, 153, 0);

	PolylineConnection figure;
	LinkPart editPart;
	final double ratio;
	final ProcessNode sourceNode;
	final ProcessNode targetNode;
	final ProcessLink processLink;

	public Link(ProcessNode sourceNode, ProcessNode targetNode, ProcessLink processLink, double ratio) {
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
		if (!(obj instanceof Link))
			return false;
		Link other = (Link) obj;
		return Objects.equals(this.processLink, other.processLink);
	}

	public void link() {
		sourceNode.add(this);
		if (sourceNode != targetNode)
			targetNode.add(this);
	}

	void setSelected(int value) {
		editPart.setSelected(value);
	}
	
	boolean isSelected() {
		return editPart.getSelected() != EditPart.SELECTED_NONE;
	}

	boolean isVisible() {
		return figure != null ? figure.isVisible() : false;
	}

	int getWidth() {
		double dWidth = ratio * Link.MAXIMIM_WIDTH;
		int width = (int) Math.ceil(Math.abs(dWidth));
		if (width == 0)
			return 1;
		if (width > Link.MAXIMIM_WIDTH)
			return Link.MAXIMIM_WIDTH;
		return width;
	}

	Color getColor() {
		RGB rgb = FaviColor.getForContribution(ratio);
		return Colors.get(rgb);
	}

}
