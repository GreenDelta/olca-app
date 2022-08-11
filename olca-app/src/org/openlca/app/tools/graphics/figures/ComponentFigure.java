package org.openlca.app.tools.graphics.figures;

import org.eclipse.draw2d.Figure;
import org.openlca.app.tools.graphics.model.Component;

public class ComponentFigure extends Figure {

	private final Component component;

	public ComponentFigure(Component node) {
		this.component = node;
	}

	public Component getComponent() {
		return component;
	}

}
