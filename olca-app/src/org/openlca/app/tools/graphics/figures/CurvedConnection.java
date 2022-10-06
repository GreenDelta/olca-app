package org.openlca.app.tools.graphics.figures;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;

import static org.eclipse.draw2d.PositionConstants.*;
import static org.eclipse.draw2d.PositionConstants.NORTH;
import static org.eclipse.swt.SWT.HIGH;

public class CurvedConnection extends SelectableConnection {

	private static final double FLATNESS = 0.1;

	private static final PrecisionPoint START_POINT = new PrecisionPoint();
	private static final PrecisionPoint END_POINT = new PrecisionPoint();
	private int offset = 20;
	private int tangent = 1;


	private final int orientation;

	public CurvedConnection(int orientation, Color color, Color colorSelected) {
		super(color, colorSelected);
		this.orientation = orientation;
	}

	@Override
	protected void outlineShape(Graphics g) {
		g.setInterpolation(HIGH);
		START_POINT.setLocation(getStart());
		END_POINT.setLocation(getEnd());

		var controlPoints = getControlPoints(offset);
		var tangentPoints = getControlPoints(tangent);

		var curve = new CubicCurve2D.Float(
				START_POINT.x, START_POINT.y,
				controlPoints.getLeft().x, controlPoints.getLeft().y,
				controlPoints.getRight().x, controlPoints.getRight().y,
				END_POINT.x, END_POINT.y);

		var pathIterator = curve.getPathIterator(null, FLATNESS);

		var path = new Path(Display.getCurrent());
		path.moveTo(getStart().x, getStart().y);
		path.moveTo(tangentPoints.getLeft().x, tangentPoints.getLeft().y);

		while (!pathIterator.isDone()) {
			var point = nextPoint(pathIterator);
			path.lineTo((float) point.preciseX(), (float) point.preciseY());
			pathIterator.next();
		}

		path.moveTo(tangentPoints.getRight().x, tangentPoints.getRight().y);
		path.lineTo(getEnd().x, getEnd().y);
		g.drawPath(path);
		path.dispose();
	}


	private ImmutablePair<PrecisionPoint, PrecisionPoint> getControlPoints(
			int offset) {
		switch (orientation) {
			case EAST -> {
				return ImmutablePair.of(
						new PrecisionPoint(START_POINT.x + offset, START_POINT.y),
						new PrecisionPoint(END_POINT.x - offset, END_POINT.y)
				);
			}
			case WEST -> {
				return ImmutablePair.of(
						new PrecisionPoint(START_POINT.x - offset, START_POINT.y),
						new PrecisionPoint(START_POINT.x - offset, START_POINT.y)
				);
			}
			case SOUTH -> {
				return ImmutablePair.of(
						new PrecisionPoint(START_POINT.x, START_POINT.y + offset),
						new PrecisionPoint(END_POINT.x, END_POINT.y - offset)
				);
			}
			case NORTH -> {
				return ImmutablePair.of(
						new PrecisionPoint(START_POINT.x, START_POINT.y - offset),
						new PrecisionPoint(END_POINT.x, END_POINT.y + offset)
				);
			}
			default -> {
				return ImmutablePair.of(new PrecisionPoint(), new PrecisionPoint());
			}
		}
	}

	private Point nextPoint(PathIterator pathIterator) {
		double[] coordinates = new double[6];
		var point = new PrecisionPoint();
		pathIterator.currentSegment(coordinates);
		point.setPreciseLocation(coordinates[0], coordinates[1]);
		return point;
	}

	@Override
	public Rectangle getBounds() {
		var bounds = super.getBounds();
		bounds.expand(2 * offset, 2 * offset);
		return bounds;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

}
