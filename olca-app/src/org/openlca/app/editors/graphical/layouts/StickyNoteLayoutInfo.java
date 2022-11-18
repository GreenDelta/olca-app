package org.openlca.app.editors.graphical.layouts;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graphical.model.Node;

import static org.openlca.app.tools.graphics.layouts.GraphLayout.DEFAULT_LOCATION;

public class StickyNoteLayoutInfo {

	public Point location;
	public Dimension size;
	public boolean minimized;
	public String title;
	public String content;

	public StickyNoteLayoutInfo() {}

	public StickyNoteLayoutInfo(Point location, Dimension size,
		boolean minimized, String title, String content) {
		this.location = location == null ? DEFAULT_LOCATION : location;
		this.size = size == null ? Node.DEFAULT_SIZE : size;
		this.minimized = minimized;
		this.title = title;
		this.content = content;
	}

	public StickyNoteLayoutInfo(org.eclipse.swt.graphics.Point location,
		Dimension size, boolean minimized, String title, String content) {
		this(new Point(location.x, location.y), size, minimized, title, content);
	}

	public StickyNoteLayoutInfo(Point location, Dimension size) {
		this(location, size, false, "", "");
	}

}
