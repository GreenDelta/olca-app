package org.openlca.app.tools.graphics.layouts;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;

import static org.eclipse.draw2d.PositionConstants.HORIZONTAL;

public class CurvedConnectionRouter extends BendpointConnectionRouter {

	private static final double FLATNESS = 0.5;

	private static final PrecisionPoint START_POINT = new PrecisionPoint();
	private static final PrecisionPoint END_POINT = new PrecisionPoint();
	private static final PrecisionPoint CTRL1 = new PrecisionPoint();
	private static final PrecisionPoint CTRL2 = new PrecisionPoint();
	private static int offset = 20;

	private final int orientation;

	public CurvedConnectionRouter(int orientation) {
		this.orientation = orientation;
	}

	@Override
	public void route(Connection conn) {
		var points = conn.getPoints();
		points.removeAllPoints();

		var ref1 = conn.getTargetAnchor().getReferencePoint();
		var ref2 = conn.getSourceAnchor().getReferencePoint();

		START_POINT.setLocation(conn.getSourceAnchor().getLocation(ref1));
		conn.translateToRelative(START_POINT);
		END_POINT.setLocation(conn.getTargetAnchor().getLocation(ref2));
		conn.translateToRelative(END_POINT);

		setControlPoints();

		var curve = new CubicCurve2D.Float(
				START_POINT.x, START_POINT.y,
				CTRL1.x, CTRL1.y,
				CTRL2.x, CTRL2.y,
				END_POINT.x, END_POINT.y);

		var pathIterator = curve.getPathIterator(null, FLATNESS);

		while (!pathIterator.isDone()) {
			points.addPoint(nextPoint(pathIterator));
			pathIterator.next();
		}

		conn.setPoints(points);
	}

	private void setControlPoints() {
		if (orientation == HORIZONTAL) {
			CTRL1.setLocation(START_POINT.x + offset, START_POINT.y);
			CTRL2.setLocation(END_POINT.x - offset, END_POINT.y);
		} else {
			CTRL1.setLocation(START_POINT.x, START_POINT.y + offset);
			CTRL2.setLocation(END_POINT.x, END_POINT.y - offset);
		}

	}

	private Point nextPoint(PathIterator pathIterator) {
		double[] coordinates = new double[6];
		var point = new PrecisionPoint();
		pathIterator.currentSegment(coordinates);
		point.setPreciseLocation(coordinates[0], coordinates[1]);
		return point;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

}
