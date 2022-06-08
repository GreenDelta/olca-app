package org.openlca.app.editors.graphical_legacy.view;

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
		g.drawRoundRectangle(tempRect, 15, 15);
	}
}
