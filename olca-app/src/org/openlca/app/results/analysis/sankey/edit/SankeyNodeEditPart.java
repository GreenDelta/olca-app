package org.openlca.app.results.analysis.sankey.edit;

import java.beans.PropertyChangeEvent;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.openlca.app.App;
import org.openlca.app.components.graphics.model.Component;
import org.openlca.app.results.analysis.sankey.figures.SankeyNodeFigure;
import org.openlca.app.results.analysis.sankey.model.SankeyNode;

public class SankeyNodeEditPart extends AbstractVertexEditPart<SankeyNode> {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE,
				new SankeyComponentEditPolicy());
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE,
				new SankeyNodeSelectionEditPolicy());
	}

	@Override
	public void performRequest(Request request) {
		if (request.getType() == RequestConstants.REQ_OPEN) {
			App.open(getModel().product.provider());
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (Component.SIZE_PROP.equals(prop)
				|| Component.LOCATION_PROP.equals(prop))
			refreshVisuals();
		else super.propertyChange(evt);
	}

	@Override
	protected void refreshVisuals() {
		var bounds = new Rectangle(getModel().getLocation(), getModel().getSize());
		((GraphicalEditPart) getParent()).setLayoutConstraint(this,
				getFigure(), bounds);
		super.refreshVisuals();
	}

	@Override
	protected IFigure createFigure() {
		return new SankeyNodeFigure(getModel());
	}

	@Override
	public SankeyNodeFigure getFigure() {
		return (SankeyNodeFigure) super.getFigure();
	}

	@Override
	public String toString() {
		return "EditPart of " + getModel();
	}

}
