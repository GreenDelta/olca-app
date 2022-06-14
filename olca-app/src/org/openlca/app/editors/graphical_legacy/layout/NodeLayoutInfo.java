package org.openlca.app.editors.graphical_legacy.layout;

import org.eclipse.draw2d.geometry.Rectangle;

public class NodeLayoutInfo {

	/**
	 * The ID of the process or product system.
	 */
	public String id;

	public final Rectangle box;
	public boolean minimized;
	public boolean expandedLeft;
	public boolean expandedRight;
	public boolean marked;

	public NodeLayoutInfo() {
		this.box = new Rectangle();
	}

}
