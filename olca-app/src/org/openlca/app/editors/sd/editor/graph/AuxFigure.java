package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.ToolbarLayout;

class AuxFigure extends Figure {

	private final Label label = new Label();

	AuxFigure() {
		var layout = new ToolbarLayout();
		layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
		layout.setSpacing(4);
		setLayoutManager(layout);
		add(label);
		add(new FixedCircle());
	}

	void setText(String text) {
		label.setText(text);
	}

	private static class FixedCircle extends Shape {

		private FixedCircle() {
			setPreferredSize(8, 8);
			setBackgroundColor(ColorConstants.black);
			setForegroundColor(ColorConstants.black);
		}

		@Override
		protected void fillShape(Graphics g) {
			var r = getBounds();
			int x = r.x + (r.width - 8) / 2;
			int y = r.y + (r.height - 8) / 2;
			g.setBackgroundColor(getForegroundColor());
			g.fillOval(x, y, 8, 8);
		}

		@Override
		protected void outlineShape(Graphics g) {
		}
	}

}
