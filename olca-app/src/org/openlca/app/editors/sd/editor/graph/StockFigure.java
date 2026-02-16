package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Theme.Box;

class StockFigure extends Figure {

	private final Label label = new Label();

	StockFigure(Theme theme) {
		var layout = new ToolbarLayout();
		layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
		setLayoutManager(layout);
		var borderColor = theme.boxBorderColor(Box.DEFAULT);
		var bgColor = theme.boxBackgroundColor(Box.DEFAULT);
		var textColor = theme.boxFontColor(Box.DEFAULT);
		setBorder(new LineBorder(borderColor, 1));
		setBackgroundColor(bgColor);
		label.setForegroundColor(textColor);
		setOpaque(true);
		add(label);
	}

	void setText(String text) {
		label.setText(text);
	}

}
