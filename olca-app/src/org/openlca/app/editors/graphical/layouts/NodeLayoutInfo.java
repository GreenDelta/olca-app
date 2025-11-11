package org.openlca.app.editors.graphical.layouts;

import static org.openlca.app.components.graphics.layouts.GraphLayout.DEFAULT_LOCATION;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graphical.model.Node;

public class NodeLayoutInfo {

	/**
	 * The ID of the process or product system.
	 */
	public String id;

	public Point location;
	public Dimension size;
	public boolean minimized;
	public boolean expandedLeft;
	public boolean expandedRight;

	public NodeLayoutInfo() {}

	public NodeLayoutInfo(Point location, Dimension size, boolean minimized,
												boolean expandedLeft, boolean expandedRight) {
		this.location = location == null ? DEFAULT_LOCATION : location;
		this.size = size == null ? Node.DEFAULT_SIZE : size;
		this.minimized = minimized;
		this.expandedLeft = expandedLeft;
		this.expandedRight = expandedRight;
	}

	public NodeLayoutInfo(org.eclipse.swt.graphics.Point location, Dimension size,
												boolean minimized, boolean expandedLeft,
												boolean expandedRight) {
		this(new Point(location.x, location.y), size, minimized, expandedLeft,
			expandedRight);
	}

}
