package org.openlca.app.editors.graph.edit;

import java.beans.PropertyChangeEvent;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.openlca.app.editors.graph.figures.IOPaneFigure;
import org.openlca.app.editors.graph.model.IOPane;
import org.openlca.app.editors.graph.model.GraphComponent;

public class IOPaneEditPart extends AbstractComponentEditPart<IOPane> {

	@Override
	protected IFigure createFigure() {
		return new IOPaneFigure(getModel());
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new MinMaxComponentEditPolicy());
	}

	@Override
	protected void refreshVisuals() {
		super.refreshVisuals();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.SIZE_PROP.equals(prop)
			|| GraphComponent.LOCATION_PROP.equals(prop)) {
			refreshVisuals();
		}
	}

	@Override
	public IFigure getContentPane() {
		return getFigure().getContentPane();
	}

	@Override
	public IOPaneFigure getFigure() {
		return (IOPaneFigure) super.getFigure();
	}

}
