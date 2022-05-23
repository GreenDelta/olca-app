package org.openlca.app.editors.graph.layouts;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graph.model.Node;

public class NodeLayoutInfo {

	/**
	 * The ID of the process or product system.
	 */
	public String id;

	public final Rectangle box;
	public boolean minimized;
	public boolean expandedLeft;
	public boolean expandedRight;

	public NodeLayoutInfo() {
		this.box = new Rectangle();
	}

	public NodeLayoutInfo(Point location, Dimension size, boolean minimized,
												boolean expandedLeft, boolean expandedRight) {
		this.box = size == null
			? new Rectangle(location, Node.DEFAULT_MINIMIZED_SIZE)
			: new Rectangle(location, size);
		this.minimized = minimized;
		this.expandedLeft = expandedLeft;
		this.expandedRight = expandedRight;
	}

}
