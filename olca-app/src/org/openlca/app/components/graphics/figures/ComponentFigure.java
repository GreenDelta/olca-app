package org.openlca.app.components.graphics.figures;

import org.eclipse.draw2d.Figure;
import org.openlca.app.components.graphics.model.Component;

public class ComponentFigure extends Figure {

	private final Component component;

	public ComponentFigure(Component component) {
		this.component = component;
	}

	public Component getComponent() {
		return component;
	}

}
