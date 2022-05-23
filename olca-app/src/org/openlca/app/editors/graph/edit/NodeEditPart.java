package org.openlca.app.editors.graph.edit;


import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.CommandStack;
import org.openlca.app.editors.graph.figures.GridPos;
import org.openlca.app.editors.graph.figures.MaximizedNodeFigure;
import org.openlca.app.editors.graph.figures.MinimizedNodeFigure;
import org.openlca.app.editors.graph.model.GraphComponent;
import org.openlca.app.editors.graph.model.Node;

abstract class NodeEditPart extends AbstractNodeEditPart<Node> {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new MinMaxComponentEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.SIZE_PROP.equals(prop)
			|| GraphComponent.LOCATION_PROP.equals(prop))
			refreshVisuals();
		else super.propertyChange(evt);
	}

	@Override
	public void performRequest(Request request) {
		if (request.getType() == RequestConstants.REQ_OPEN) {
			CommandStack stack = getViewer().getEditDomain().getCommandStack();
			var command = getCommand(request);
			stack.execute(command);
		}
	}

	@Override
	protected void refreshVisuals() {
		var bounds = new Rectangle(getModel().getLocation(),
			getModel().getSize());
		((GraphicalEditPart) getParent()).setLayoutConstraint(this,
			getFigure(), bounds);
	}

	public static class Maximized extends NodeEditPart {

		@Override
		protected IFigure createFigure() {
			return new MaximizedNodeFigure(getModel());
		}

		@Override
		public IFigure getContentPane() {
			return ((MaximizedNodeFigure) getFigure()).getContentPane();
		}

		@Override
		protected void addChildVisual(EditPart childEditPart, int index) {
			IFigure child = ((GraphicalEditPart) childEditPart).getFigure();
			getContentPane().add(child, GridPos.fillTop(), index);
		}

	}

	public static class Minimized extends NodeEditPart {

		@Override
		protected IFigure createFigure() {
			return new MinimizedNodeFigure(getModel());
		}

		@Override
		protected List<? extends GraphComponent> getModelChildren() {
			return Collections.emptyList();
		}

	}

}
