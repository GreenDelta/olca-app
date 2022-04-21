package org.openlca.app.editors.graphical.view;

import org.eclipse.draw2d.geometry.Dimension;

public record Corners(Dimension topLeft, Dimension topRight, Dimension bottomLeft,
											Dimension bottomRight) {

	public static final Dimension ANGLE_CORNER = new Dimension(0, 0);

	public static Corners fullRoundedCorners(Dimension dimension) {
		return new Corners(
			dimension,
			dimension,
			dimension,
			dimension);
	}

	public static Corners topRoundedCorners(Dimension dimension) {
		return new Corners(
			dimension,
			dimension,
			ANGLE_CORNER,
			ANGLE_CORNER);
	}
}
