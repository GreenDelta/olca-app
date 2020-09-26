package org.openlca.app.results.analysis.sankey.model;

import java.util.Objects;

import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.util.Colors;

public class Link {

	public static final int MAXIMIM_WIDTH = 45;
	public static final Color HIGHLIGHT_COLOR = Colors.get(255, 153, 0);

	PolylineConnection figure;
	LinkPart editPart;
	final double ratio;
	public final ProcessNode sourceNode;
	public final ProcessNode targetNode;

	public Link(ProcessNode sourceNode, ProcessNode targetNode, double ratio) {
		this.ratio = ratio;
		this.sourceNode = sourceNode;
		this.targetNode = targetNode;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (!(o instanceof Link))
			return false;
		var other = (Link) o;
		return Objects.equals(
				this.sourceNode.product,
				other.sourceNode.product)
				&& Objects.equals(
				this.targetNode.product,
				other.targetNode.product);
	}

	public void link() {
		sourceNode.links.add(this);
		if (sourceNode != targetNode)
			targetNode.links.add(this);
	}

	void setSelected(int value) {
		editPart.setSelected(value);
	}

	boolean isVisible() {
		return figure != null && figure.isVisible();
	}

	int getWidth() {
		double dWidth = ratio * Link.MAXIMIM_WIDTH;
		int width = (int) Math.ceil(Math.abs(dWidth));
		if (width == 0)
			return 1;
		return Math.min(width, Link.MAXIMIM_WIDTH);
	}

	Color getColor() {
		return Colors.getForContribution(ratio);
	}

}
