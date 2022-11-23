package org.openlca.app.tools.graphics.figures;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.RotatableDecoration;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;
import java.util.Objects;

import static org.eclipse.draw2d.ConnectionLocator.SOURCE;
import static org.eclipse.draw2d.ConnectionLocator.TARGET;
import static org.eclipse.draw2d.PositionConstants.*;
import static org.eclipse.draw2d.PositionConstants.NORTH;
import static org.eclipse.swt.SWT.HIGH;

public class Connection extends SelectableConnection {

	public static final String ROUTER_NULL = "Straight line";
	public static final String ROUTER_CURVE = "Curve";
	public static final String ROUTER_MANHATTAN = "Manhattan";

	private static final double FLATNESS = 0.1;

	private int offset = 20;
	private int tangent = 1;

	private final int orientation;
	private final String type;
	private RotatableDecoration startArrow, endArrow;

	public Connection(String type, int orientation, Color color,
		Color colorSelected) {
		super(color, colorSelected);
		this.orientation = orientation;
		this.type = type;
	}

	@Override
	protected void outlineShape(Graphics g) {
		g.setInterpolation(HIGH);

		if (Objects.equals(type, ROUTER_MANHATTAN)) {
			g.drawPolyline(getPoints());
			return;
		}

		var path = new Path(Display.getCurrent());
		path.moveTo(getStart().x, getStart().y);

		if (Objects.equals(type, ROUTER_CURVE)) {
			var pathIterator = getPathIterator();

			var tangentPoints = getControlPoints(tangent);
			path.lineTo(tangentPoints.getLeft().x, tangentPoints.getLeft().y);

			while (!pathIterator.isDone()) {
				var point = nextPoint(pathIterator);
				path.lineTo((float) point.preciseX(), (float) point.preciseY());
				pathIterator.next();
			}

			path.lineTo(tangentPoints.getRight().x, tangentPoints.getRight().y);
		}

		path.lineTo(getEnd().x, getEnd().y);

		g.drawPath(path);
		path.dispose();
	}

	PathIterator getPathIterator() {
		var initPoints = getControlPoints(tangent);
		var controlPoints = getControlPoints(offset);
		var curve = new CubicCurve2D.Float(
				initPoints.getLeft().x, initPoints.getLeft().y,
				controlPoints.getLeft().x, controlPoints.getLeft().y,
				controlPoints.getRight().x, controlPoints.getRight().y,
				initPoints.getRight().x, initPoints.getRight().y);
		return curve.getPathIterator(null, FLATNESS);
	}


	ImmutablePair<PrecisionPoint, PrecisionPoint> getControlPoints(
			int offset) {
		switch (orientation) {
			case EAST -> {
				return ImmutablePair.of(
						new PrecisionPoint(getStart().x + offset, getStart().y),
						new PrecisionPoint(getEnd().x - offset, getEnd().y)
				);
			}
			case WEST -> {
				return ImmutablePair.of(
						new PrecisionPoint(getStart().x - offset, getStart().y),
						new PrecisionPoint(getEnd().x + offset, getEnd().y)
				);
			}
			case SOUTH -> {
				return ImmutablePair.of(
						new PrecisionPoint(getStart().x, getStart().y + offset),
						new PrecisionPoint(getEnd().x, getEnd().y - offset)
				);
			}
			case NORTH -> {
				return ImmutablePair.of(
						new PrecisionPoint(getStart().x, getStart().y - offset),
						new PrecisionPoint(getEnd().x, getEnd().y + offset)
				);
			}
			default -> {
				return ImmutablePair.of(new PrecisionPoint(), new PrecisionPoint());
			}
		}
	}

	static Point nextPoint(PathIterator pathIterator) {
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

	public String getType() {
		return type;
	}

	@Override
	public void setSourceDecoration(RotatableDecoration dec) {
		if (startArrow == dec)
			return;
		if (startArrow != null)
			remove(startArrow);
		startArrow = dec;
		if (startArrow != null)
			add(startArrow,
					new TangentialArrowLocator(this, SOURCE));
	}

	@Override
	protected RotatableDecoration getSourceDecoration() {
		return startArrow;
	}

	@Override
	public void setTargetDecoration(RotatableDecoration dec) {
		if (endArrow == dec)
			return;
		if (endArrow != null)
			remove(endArrow);
		endArrow = dec;
		if (endArrow != null)
			add(endArrow, new TangentialArrowLocator(this, TARGET));
	}

	@Override
	protected RotatableDecoration getTargetDecoration() {
		return endArrow;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

}
