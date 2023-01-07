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
import org.openlca.app.tools.graphics.model.BaseComponent;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;
import java.util.List;
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
	public static final List ROUTERS = List.of(ROUTER_NULL, ROUTER_CURVE,
			ROUTER_MANHATTAN);

	private static final double FLATNESS = 0.1;
	private static final int CHILDREN_LIMIT = 100;
	private final BaseComponent base;

	private int offset = 20;
	private int tangent = 1;

	private final int orientation;
	private final String type;
	private RotatableDecoration startArrow, endArrow;

	public Connection(String type, int orientation, Color color,
		Color colorSelected, BaseComponent base) {
		super(color, colorSelected);
		this.orientation = orientation;
		this.type = ROUTERS.contains(type) ? type : ROUTER_CURVE;
		this.base = base;
	}

	@Override
	protected void outlineShape(Graphics g) {
		g.setInterpolation(HIGH);
		// When the graphical interface is too packed, ROUTER_NULL is used.
		var accelerate = base.getChildren().size() > CHILDREN_LIMIT;

		if (Objects.equals(type, ROUTER_MANHATTAN)) {
			g.drawPolyline(getPoints());
			return;
		}

		var path = new Path(Display.getCurrent());

		if (Objects.equals(type, ROUTER_NULL) || accelerate) {
			var points = getControlPoints(-getLineWidth() / 2);
			path.moveTo(points.getLeft().x, points.getLeft().y);
			path.lineTo(points.getRight().x, points.getRight().y);
		}

		if (Objects.equals(type, ROUTER_CURVE) && !accelerate) {
			path.moveTo(getStart().x, getStart().y);

			var tangentPoints = getControlPoints(tangent);
			path.lineTo(tangentPoints.getLeft().x, tangentPoints.getLeft().y);

			var pathIterator = getPathIterator();
			while (!pathIterator.isDone()) {
				var point = nextPoint(pathIterator);
				path.lineTo((float) point.preciseX(), (float) point.preciseY());
				pathIterator.next();
			}

			path.lineTo(tangentPoints.getRight().x, tangentPoints.getRight().y);
			path.lineTo(getEnd().x, getEnd().y);
		}

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
		var start = getStart();
		var end = getEnd();
		switch (orientation) {
			case EAST -> {
				return ImmutablePair.of(
						new PrecisionPoint(start.x + offset, start.y),
						new PrecisionPoint(end.x - offset, end.y)
				);
			}
			case WEST -> {
				return ImmutablePair.of(
						new PrecisionPoint(start.x - offset, start.y),
						new PrecisionPoint(end.x + offset, end.y)
				);
			}
			case SOUTH -> {
				return ImmutablePair.of(
						new PrecisionPoint(start.x, start.y + offset),
						new PrecisionPoint(end.x, end.y - offset)
				);
			}
			case NORTH -> {
				return ImmutablePair.of(
						new PrecisionPoint(start.x, start.y - offset),
						new PrecisionPoint(end.x, end.y + offset)
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
