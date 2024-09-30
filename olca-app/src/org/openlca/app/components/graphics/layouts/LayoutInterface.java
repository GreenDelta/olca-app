package org.openlca.app.components.graphics.layouts;

import org.openlca.app.components.graphics.figures.ComponentFigure;
import org.openlca.app.components.graphics.model.Component;

public interface LayoutInterface {

	ComponentFigure figureOf(Component node);

	Component getReferenceNode();

  ComponentFigure getReferenceFigure();

}
