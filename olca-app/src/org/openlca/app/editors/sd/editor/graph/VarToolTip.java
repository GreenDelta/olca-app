package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.openlca.app.editors.sd.SdVars;
import org.openlca.sd.eqn.Var;

class VarToolTip extends Figure {

	VarToolTip(Var v) {
		var layout = new ToolbarLayout();
		layout.setSpacing(2);
		setLayoutManager(layout);
		setBorder(new MarginBorder(5));
		add(new Label("Variable: " + v.name().label()));
		add(new Label("Type: " + SdVars.typeOf(v)));
		add(new Label("Cell Type: " + SdVars.cellTypeOf(v)));
		if (v.unit() != null && !v.unit().isBlank()) {
			add(new Label("Unit: " + v.unit()));
		}
	}

}
