package org.openlca.app.tools.graphics.figures;

import org.eclipse.draw2d.Figure;
import org.openlca.app.tools.graphics.model.Component;

public class ComponentFigure extends Figure {

	private final Component component;

	public ComponentFigure(Component component) {
		this.component = component;
	}

	public Component getComponent() {
		return component;
	}

}
