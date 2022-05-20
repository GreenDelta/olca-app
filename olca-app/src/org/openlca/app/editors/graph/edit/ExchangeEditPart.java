package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.openlca.app.editors.graph.figures.ExchangeFigure;
import org.openlca.app.editors.graph.model.ExchangeItem;

import java.beans.PropertyChangeEvent;

public class ExchangeEditPart extends AbstractNodeEditPart<ExchangeItem> {

	@Override
	protected IFigure createFigure() {
		return new ExchangeFigure(getModel());
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
			new ExchangeItemEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		super.propertyChange(evt);
	}

	@Override
	public DragTracker getDragTracker(Request req) {
		if (req instanceof SelectionRequest sel) {
			if (sel.getLastButtonPressed() == 3)
				return super.getDragTracker(req);
		}
		return new ConnectionDragCreationTool();
	}

}
