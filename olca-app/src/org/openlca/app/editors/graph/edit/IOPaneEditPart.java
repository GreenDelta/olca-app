package org.openlca.app.editors.graph.edit;

import java.beans.PropertyChangeEvent;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.openlca.app.editors.graph.figures.GridPos;
import org.openlca.app.editors.graph.figures.IOPaneFigure;
import org.openlca.app.editors.graph.figures.NodeFigure;
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
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.SIZE_PROP.equals(prop)
			|| GraphComponent.LOCATION_PROP.equals(prop)) {
			refreshVisuals();
		}
		else super.propertyChange(evt);
	}

	@Override
	public IFigure getContentPane() {
		return getFigure().getContentPane();
	}

	@Override
	protected void addChildVisual(EditPart childEditPart, int index) {
		IFigure child = ((GraphicalEditPart) childEditPart).getFigure();
		getContentPane().add(child, GridPos.fillTop(), index);
	}

	@Override
	public IOPaneFigure getFigure() {
		return (IOPaneFigure) super.getFigure();
	}

	@Override
	public NodeEditPart getParent() {
		return (NodeEditPart) super.getParent();
	}

}
