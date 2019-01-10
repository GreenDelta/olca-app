package org.openlca.app.editors.graphical.layout;

import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class NodeLayoutInfo {

	private long id;
	private int x;
	private int y;
	private boolean minimized;
	private boolean expandedLeft;
	private boolean expandedRight;
	private boolean marked;

	public NodeLayoutInfo(long id, int x, int y, boolean minimized,
			boolean expandedLeft, boolean expandedRight, boolean marked) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.minimized = minimized;
		this.expandedLeft = expandedLeft;
		this.expandedRight = expandedRight;
		this.marked = marked;
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

	public long getId() {
		return id;
	}

	public Point getLocation() {
		return new Point(x, y);
	}

	public boolean isMinimized() {
		return minimized;
	}

	public boolean isExpandedLeft() {
		return expandedLeft;
	}

	public boolean isExpandedRight() {
		return expandedRight;
	}

	public boolean isMarked() {
		return marked;
	}

}
