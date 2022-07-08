package org.openlca.app.editors.graphical.edit;


import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.CommandStack;
import org.openlca.app.editors.graphical.figures.GridPos;
import org.openlca.app.editors.graphical.figures.MaximizedNodeFigure;
import org.openlca.app.editors.graphical.figures.MinimizedNodeFigure;
import org.openlca.app.editors.graphical.figures.NodeFigure;
import org.openlca.app.editors.graphical.model.GraphComponent;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.requests.ExpandCollapseRequest;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;

public abstract class NodeEditPart extends AbstractVertexEditPart<Node> {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new NodeEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.SIZE_PROP.equals(prop)
			|| GraphComponent.LOCATION_PROP.equals(prop)
		  || Node.EXPANDED_PROP.equals(prop))
			refreshVisuals();
		else super.propertyChange(evt);
	}

	@Override
	public void performRequest(Request request) {
		if (request.getType() == RequestConstants.REQ_OPEN) {
			CommandStack stack = getViewer().getEditDomain().getCommandStack();
			var command = getCommand(request);
			if (command != null && command.canExecute()) {
				stack.execute(command);
			}
		}
	}

	@Override
	protected void refreshVisuals() {
		var bounds = new Rectangle(getModel().getLocation(), getModel().getSize());
		((GraphicalEditPart) getParent()).setLayoutConstraint(this,
			getFigure(), bounds);
		super.refreshVisuals();
	}

	protected void addButtonActionListener(NodeFigure figure) {
		figure.inputExpandButton.addActionListener($ -> {
			var command = getCommand(new ExpandCollapseRequest(getModel(), INPUT));
			if (command.canExecute())
				getViewer().getEditDomain().getCommandStack().execute(command);
		});
		figure.outputExpandButton.addActionListener($ -> {
			var command = getCommand(new ExpandCollapseRequest(getModel(), OUTPUT));
			if (command.canExecute())
				getViewer().getEditDomain().getCommandStack().execute(command);
		});
	}

	@Override
	public NodeFigure getFigure() {
		return (NodeFigure) super.getFigure();
	}

	@Override
	public String toString() {
		return "EditPart of " + getModel();
	}

	public static class Maximized extends NodeEditPart {

		@Override
		protected IFigure createFigure() {
			var figure = new MaximizedNodeFigure(getModel());
			addButtonActionListener(figure);
			return figure;
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
			var figure = new MinimizedNodeFigure(getModel());
			addButtonActionListener(figure);
			return figure;
		}

		@Override
		protected List<? extends GraphComponent> getModelChildren() {
			return Collections.emptyList();
		}

	}

}
