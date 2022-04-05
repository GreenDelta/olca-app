package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.swt.SWT;

public class RoundBorder extends LineBorder {

	public RoundBorder(int value) {
		super(value);
	}

	@Override
	public void paint(IFigure figure, Graphics graphics, Insets insets) {
		graphics.setAntialias(SWT.ON);
		tempRect.setBounds(getPaintRectangle(figure, insets));
		if (getWidth() % 2 == 1) {
			tempRect.width--;
			tempRect.height--;
		}
		tempRect.shrink(getWidth() / 2, getWidth() / 2);
		graphics.setLineWidth(getWidth());
		graphics.setLineStyle(getStyle());
		if (getColor() != null)
			graphics.setForegroundColor(getColor());
		graphics.drawRoundRectangle(tempRect, 15, 15);
	}
}
