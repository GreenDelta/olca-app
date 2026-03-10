package org.openlca.app.editors.sd.editor.graph.view;

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PositionConstants;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Theme.Box;
import org.openlca.sd.model.Var;

public class StockFigure extends Figure {

	private final Label label = new Label();

	public StockFigure(Theme theme) {
		var layout = new BorderLayout();
		setLayoutManager(layout);
		var borderColor = theme.boxBorderColor(Box.DEFAULT);
		var bgColor = theme.boxBackgroundColor(Box.DEFAULT);
		var textColor = theme.boxFontColor(Box.DEFAULT);
		setBorder(new LineBorder(borderColor, 1));
		setBackgroundColor(bgColor);
		label.setForegroundColor(textColor);
		label.setTextAlignment(PositionConstants.CENTER);
		add(label, BorderLayout.CENTER);
		setOpaque(true);
	}

	public void setVar(Var v) {
		if (v == null) {
			label.setText("");
			setToolTip(null);
		} else {
			var name = v.name();
			label.setText(name != null ? name.label() : "");
			setToolTip(new VarToolTip(v));
		}
	}

}
