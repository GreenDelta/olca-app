package org.openlca.app.editors.graphical.layout;

import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class NodeLayoutInfo {

	/**
	 * The ID of the process or product system.
	 */
	public long id;

	public int x;
	public int y;
	public boolean minimized;
	public boolean expandedLeft;
	public boolean expandedRight;
	public boolean marked;

	public NodeLayoutInfo() {
	}

	public NodeLayoutInfo(ProcessNode node) {
		this.id = node.process.id;
		this.x = node.getXyLayoutConstraints().x;
		this.y = node.getXyLayoutConstraints().y;
		this.minimized = node.isMinimized();
		this.expandedLeft = node.isExpandedLeft();
		this.expandedRight = node.isExpandedRight();
		this.marked = node.isMarked();
	}

	public Point getLocation() {
		return new Point(x, y);
	}
}
