package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.ToolbarLayout;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Theme.Box;
import org.openlca.sd.eqn.Var;

class AuxFigure extends Figure {

	private final Label label = new Label();
	private final Theme theme;

	AuxFigure(Theme theme) {
		this.theme = theme;
		var layout = new ToolbarLayout();
		layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
		layout.setSpacing(4);
		setLayoutManager(layout);
		label.setForegroundColor(theme.boxFontColor(Box.DEFAULT));
		add(label);
		add(new FixedCircle());
	}

	void setVar(Var v) {
		if (v == null) {
			label.setText("");
			setToolTip(null);
		} else {
			var name = v.name();
			label.setText(name != null ? name.label() : "");
			setToolTip(new VarToolTip(v));
		}
	}

	private class FixedCircle extends Shape {

		private FixedCircle() {
			setPreferredSize(8, 8);
		}

		@Override
		protected void fillShape(Graphics g) {
			var r = getBounds();
			int x = r.x + (r.width - 8) / 2;
			int y = r.y + (r.height - 8) / 2;
			g.setBackgroundColor(theme.boxBorderColor(Box.RESULT));
			g.fillOval(x, y, 8, 8);
		}

		@Override
		protected void outlineShape(Graphics g) {
		}
	}

}
