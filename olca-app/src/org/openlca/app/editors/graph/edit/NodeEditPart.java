package org.openlca.app.editors.graph.edit;


import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.CommandStack;
import org.openlca.app.editors.graph.figures.MaximizedNodeFigure;
import org.openlca.app.editors.graph.figures.MinimizedNodeFigure;
import org.openlca.app.editors.graph.model.GraphComponent;
import org.openlca.app.editors.graph.model.Node;


abstract class NodeEditPart extends AbstractNodeEditPart<Node> {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new MinMaxComponentEditPolicy());
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE,
			new NodeGraphicalEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.SIZE_PROP.equals(prop)
			|| GraphComponent.LOCATION_PROP.equals(prop))
			refreshVisuals();
		else if (GraphComponent.INPUTS_PROP.equals(prop))
			refreshSourceConnections();
		else if (GraphComponent.OUTPUTS_PROP.equals(prop))
			refreshTargetConnections();
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
			var model = getModel();
			return new MaximizedNodeFigure(model.descriptor);
		}

		@Override
		public IFigure getContentPane() {
			return ((MaximizedNodeFigure) getFigure()).getContentPane();
		}

	}

	public static class Minimized extends NodeEditPart {

		@Override
		protected IFigure createFigure() {
			var model = getModel();
			return new MinimizedNodeFigure(model.descriptor);
		}

		@Override
		protected List<? extends GraphComponent> getModelChildren() {
			return Collections.emptyList();
		}

	}

}
