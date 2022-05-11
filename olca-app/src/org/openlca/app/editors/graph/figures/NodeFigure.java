package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.openlca.app.util.Colors;

public class NodeFigure extends Figure {

	private String name;

	public NodeFigure() {
		name = "Node Figure";

		setToolTip(new Label(name));
		setForegroundColor(Colors.white());
		setOpaque(true);
	}

}
