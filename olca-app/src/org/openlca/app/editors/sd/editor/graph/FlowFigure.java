package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.ToolbarLayout;

class FlowFigure extends Figure {

	private final Label label = new Label();
	private final Ellipse circle = new Ellipse();

	FlowFigure() {
		var layout = new ToolbarLayout();
		layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
		setLayoutManager(layout);
		add(label);
		circle.setSize(16, 16);
		add(circle);
	}

	void setText(String text) {
		label.setText(text);
	}

}
