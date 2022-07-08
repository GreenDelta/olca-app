package org.openlca.app.editors.graphical.edit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.openlca.app.editors.graphical.figures.ExchangeFigure;
import org.openlca.app.editors.graphical.model.ExchangeItem;

import java.beans.PropertyChangeEvent;

public class ExchangeEditPart extends AbstractVertexEditPart<ExchangeItem> {

	@Override
	protected IFigure createFigure() {
		var figure = new ExchangeFigure(getModel());
		figure.setChildren(((IOPaneEditPart) getParent()).getFigure());
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE,
			new GraphComponentEditPolicy());
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
			new ExchangeItemEditPolicy());
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new ExchangeSelectionEditPolicy());
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
