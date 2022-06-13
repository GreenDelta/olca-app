package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

public class RoundBorder extends LineBorder {

	public record Corners(Dimension topLeft, Dimension topRight, Dimension bottomLeft,
												Dimension bottomRight) {

		public static final Dimension ANGLE_CORNER = new Dimension(0, 0);

		public static Corners angleCorners() { return new Corners(
			ANGLE_CORNER,
			ANGLE_CORNER,
			ANGLE_CORNER,
			ANGLE_CORNER);
		}

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

	private final Corners corners;

	public RoundBorder(int value, Corners corners) {
		super(value);
		this.corners = corners;
	}

	@Override
	public void paint(IFigure figure, Graphics g, Insets insets) {
		g.setAntialias(SWT.ON);
		tempRect.setBounds(getPaintRectangle(figure, insets));
		if (getWidth() % 2 == 1) {
			tempRect.width--;
			tempRect.height--;
		}
		tempRect.shrink(getWidth() / 2, getWidth() / 2);
		g.setLineWidth(getWidth());
		g.setLineStyle(getStyle());
		if (getColor() != null)
			g.setForegroundColor(getColor());
		drawRoundRectangle(g, tempRect, corners);
	}

	private void drawRoundRectangle(Graphics g, Rectangle r, Corners corners){
		// draw top line and top right corner
		if (corners.topRight().width == 0 || corners.topRight().height == 0)
			// Let draw2d manage the right-angle corner.
			g.drawPolyline(new int[] {
				r.x + corners.topLeft().width / 2, r.y,
				r.x + r.width, r.y,
				r.x + r.width, r.y + 1});
		else {
			g.drawLine(
				r.x + corners.topLeft().width / 2, r.y,
				r.x + r.width - corners.topRight().width / 2, r.y);
			g.drawArc(
				r.x + r.width - corners.topRight().width, r.y,
				corners.topRight().width, corners.topRight().height,
				0, 90);
		}

		// draw right line and bottom right corner
		if (corners.bottomRight().width == 0 || corners.bottomRight().height == 0)
			// Let draw2d manage the right-angle corner.
			g.drawPolyline(new int[] {
				r.x + r.width, r.y + corners.topRight().height / 2,
				r.x + r.width, r.y + r.height,
				r.x + r.width - 1, r.y + r.height});
		else {
			g.drawLine(
				r.x + r.width, r.y + corners.topRight().height / 2,
				r.x + r.width, r.y + r.height - corners.bottomRight().height / 2);
			g.drawArc(
				r.x + r.width - corners.bottomRight().width,
				r.y + r.height - corners.bottomRight().height,
				corners.bottomRight().width, corners.bottomRight().height,
				0, -90);
		}

		// draw bottom line and bottom left corner
		if (corners.bottomLeft().width == 0 || corners.bottomLeft().height == 0)
			// Let draw2d manage the right-angle corner.
			g.drawPolyline(new int[] {
				r.x + r.width - corners.bottomRight().width / 2, r.y + r.height,
				r.x, r.y + r.height,
				r.x, r.y + r.height - 1});
		else {
			g.drawLine(
				r.x + r.width - corners.bottomRight().width / 2, r.y + r.height,
				r.x + corners.bottomLeft().width / 2, r.y + r.height);
			g.drawArc(
				r.x, r.y + r.height - corners.bottomLeft().height,
				corners.bottomLeft().width, corners.bottomLeft().height,
				-90, -90);
		}

		// draw left line and top left corner
		if (corners.topLeft().width == 0 || corners.topLeft().height == 0)
			// Let draw2d manage the right-angle corner.
			g.drawPolyline(new int[] {
				r.x, r.y + r.height - corners.bottomLeft().height / 2,
				r.x, r.y,
				r.x + 1, r.y});
		else {
			g.drawLine(
				r.x, r.y + r.height - corners.bottomLeft().height / 2,
				r.x, r.y  + corners.topLeft().height / 2);
			g.drawArc(
				r.x, r.y,
				corners.topLeft().width, corners.topLeft().height,
				90, 90);
		}
	}

}
