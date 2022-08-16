package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.editors.graphical.model.GraphLink;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;

import static org.eclipse.draw2d.PositionConstants.*;
import static org.eclipse.draw2d.PositionConstants.NORTH;
import static org.eclipse.swt.SWT.HIGH;
import static org.eclipse.swt.SWT.ON;

public class CurvedConnection extends PolylineConnection {

	private static final double FLATNESS = 0.1;

	private static final PrecisionPoint START_POINT = new PrecisionPoint();
	private static final PrecisionPoint END_POINT = new PrecisionPoint();
	private static final PrecisionPoint CTRL1 = new PrecisionPoint();
	private static final PrecisionPoint CTRL2 = new PrecisionPoint();
	private static int offset = 20;

	private final int orientation;
	private final GraphLink link;

	public CurvedConnection(GraphLink link, int orientation) {
		this.link = link;
		this.orientation = orientation;
		setTargetDecoration(new PolygonDecoration());
		setLineWidth(1);
	}

	@Override
	public void paint(Graphics g) {
		setAntialias(ON);

		var provider = link.provider();
		var theme = provider != null
				? provider.getGraph().getEditor().config.getTheme()
				: null;
		if (theme != null) {
			setForegroundColor(theme.linkColor());
		} else {
			setForegroundColor(ColorConstants.black);
		}

		super.paint(g);
	}

	@Override
	protected void outlineShape(Graphics g) {
		g.setInterpolation(HIGH);
		START_POINT.setLocation(getStart());
		END_POINT.setLocation(getEnd());

		setControlPoints();

		var curve = new CubicCurve2D.Float(
				START_POINT.x, START_POINT.y,
				CTRL1.x, CTRL1.y,
				CTRL2.x, CTRL2.y,
				END_POINT.x, END_POINT.y);

		var pathIterator = curve.getPathIterator(null, FLATNESS);

		var path = new Path(Display.getCurrent());
		path.moveTo(getStart().x, getStart().y);

		while (!pathIterator.isDone()) {
			var point = nextPoint(pathIterator);
			path.lineTo((float) point.preciseX(), (float) point.preciseY());
			pathIterator.next();
		}

		path.lineTo(getEnd().x, getEnd().y);
		g.drawPath(path);
		path.dispose();
	}


	private void setControlPoints() {
		switch (orientation) {
			case EAST -> {
				CTRL1.setLocation(START_POINT.x + offset, START_POINT.y);
				CTRL2.setLocation(END_POINT.x - offset, END_POINT.y);
			}
			case WEST -> {
				CTRL1.setLocation(START_POINT.x - offset, START_POINT.y);
				CTRL2.setLocation(END_POINT.x + offset, END_POINT.y);
			}
			case SOUTH -> {
				CTRL1.setLocation(START_POINT.x, START_POINT.y + offset);
				CTRL2.setLocation(END_POINT.x, END_POINT.y - offset);
			}
			case NORTH -> {
				CTRL1.setLocation(START_POINT.x, START_POINT.y - offset);
				CTRL2.setLocation(END_POINT.x, END_POINT.y + offset);
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

}
