package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.ToolbarLayout;

class StockFigure extends Figure {

	private final Label label = new Label();

	StockFigure() {
		var layout = new ToolbarLayout();
		layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
		setLayoutManager(layout);
		setBorder(new LineBorder(ColorConstants.black, 1));
		setBackgroundColor(ColorConstants.white);
		setOpaque(true);
		add(label);
	}

	void setText(String text) {
		label.setText(text);
	}

}
