package org.openlca.app.tools.graphics.layouts;

import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.model.Component;

public interface LayoutInterface {

	ComponentFigure figureOf(Component node);

	Component getReferenceNode();

  ComponentFigure getReferenceFigure();

}
