package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Rectangle;

class FlowFigure extends Figure {

	private final Label label = new Label();
	private final ValveFigure valve = new ValveFigure();

	FlowFigure() {
		var layout = new ToolbarLayout();
		layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
		setLayoutManager(layout);
		add(label);
		valve.setPreferredSize(24, 24);
		add(valve);
	}

	void setText(String text) {
		label.setText(text);
	}

	private static class ValveFigure extends Shape {

		@Override
		protected void fillShape(Graphics g) {
			// use standard background
		}

		@Override
		protected void outlineShape(Graphics g) {
			Rectangle r = getBounds();
			int x = r.x;
			int y = r.y;
			int w = r.width;
			int h = r.height;

			int o = h / 10;
			int midx = x + w / 2;
			int midy = y + h / 2;

			// top bars
			g.drawLine(x, y, x + w, y);
			g.drawLine(x, y + o, x + w, y + o);

			// bottom bars
			g.drawLine(x, y + h - o, x + w, y + h - o);
			g.drawLine(x, y + h, x + w, y + h);

			// left sides
			g.drawLine(x, y + o, midx - o, midy);
			g.drawLine(midx - o, midy, x, y + h - o);

			// right sides
			g.drawLine(x + w, y + o, midx + o, midy);
			g.drawLine(midx + o, midy, x + w, y + h - o);
		}
	}

}
