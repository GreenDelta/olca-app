package org.openlca.app.editors.sd.editor.graph;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

class GraphPart extends AbstractGraphicalEditPart {

	@Override
	protected IFigure createFigure() {
		var figure = new FreeformLayer();
		figure.setLayoutManager(new FreeformLayout());
		return figure;
	}

	@Override
	protected void createEditPolicies() {
	}

	@Override
	protected List<?> getModelChildren() {
		var model = (GraphModel) getModel();
		if (model == null)
			return Collections.emptyList();
		return model.stocks;
	}

}
