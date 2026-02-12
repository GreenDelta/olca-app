package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

class GraphPart extends AbstractGraphicalEditPart {

	@Override
	protected IFigure createFigure() {
		return new FreeformLayer();
	}

	@Override
	protected void createEditPolicies() {
	}

}
