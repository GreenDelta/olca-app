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
import org.openlca.app.editors.graphical.requests.ExpansionRequest;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_LAYOUT;

public abstract class NodeEditPart extends AbstractNodeEditPart<Node> {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new NodeEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.SIZE_PROP.equals(prop)
			|| GraphComponent.LOCATION_PROP.equals(prop)
			|| Node.EXPANSION_PROP.equals(prop))
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
		super.refreshVisuals();
	}

	protected void addButtonActionListener(NodeFigure figure) {
		figure.inputExpandButton.addActionListener($ -> {
			var command = getCommand(new ExpansionRequest(getModel(), INPUT));
			getViewer().getEditDomain().getCommandStack().execute(command);

			// The layout command has to be executed after creating or deleting the
			// nodes, hence cannot be executed within a CompoundCommand.
			var layoutCommand = getParent().getCommand(new Request(REQ_LAYOUT));
			if (layoutCommand.canExecute()) {
				var stack = (CommandStack) getModel().editor
					.getAdapter(CommandStack.class);
				stack.execute(layoutCommand);
			}
		});

		figure.outputExpandButton.addActionListener($ -> {
			var command = getCommand(new ExpansionRequest(getModel(), OUTPUT));
			getViewer().getEditDomain().getCommandStack().execute(command);

			// The layout command has to be executed after creating or deleting the
			// nodes, hence cannot be executed within a CompoundCommand.
			var layoutCommand = getParent().getCommand(new Request(REQ_LAYOUT));
			if (layoutCommand.canExecute()) {
				var stack = (CommandStack) getModel().editor
					.getAdapter(CommandStack.class);
				stack.execute(layoutCommand);
			}
		});
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
