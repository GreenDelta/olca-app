package org.openlca.app.editors.graphical.edit;


import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.tools.TargetingTool;
import org.openlca.app.tools.graphics.figures.GridPos;
import org.openlca.app.editors.graphical.figures.MaximizedNodeFigure;
import org.openlca.app.editors.graphical.figures.MinimizedNodeFigure;
import org.openlca.app.editors.graphical.figures.NodeFigure;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.requests.ExpandCollapseRequest;
import org.openlca.app.tools.graphics.model.Side;


public abstract class NodeEditPart extends AbstractVertexEditPart<Node> {

	@Override
	public DragTracker getDragTracker(Request request) {
		return new org.eclipse.gef.tools.DragEditPartsTracker(this) {
			/**
			 * This method is overriden to remove the use of
			 * <code>getCurrentViewer().reveal(getSourceEditPart());</code> in
			 * SelectEditPartTracker that moves the Canvas when clicking on a Node.
			 * @param button
			 *            the button being released
			 * @return
			 */
			@Override
			protected boolean handleButtonUp(int button) {
				if (stateTransition(STATE_DRAG_IN_PROGRESS, STATE_TERMINAL)) {
					eraseSourceFeedback();
					eraseTargetFeedback();
					performDrag();
					return true;
				}
				if (isInState(STATE_DRAG)) {
					performSelection();
					if (getFlag(TargetingTool.MAX_FLAG << 2))
						performDirectEdit();
					setState(STATE_TERMINAL);
					return true;
				}
				return false;
			}
		};
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new NodeEditPolicy());
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE,
				new NodeSelectionEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (Component.SIZE_PROP.equals(prop)
				|| Component.LOCATION_PROP.equals(prop)
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
			var command = getCommand(
					new ExpandCollapseRequest(getModel(), Side.INPUT, false));
			if (command.canExecute())
				getViewer().getEditDomain().getCommandStack().execute(command);
		});
		figure.outputExpandButton.addActionListener($ -> {
			var command = getCommand(
					new ExpandCollapseRequest(getModel(), Side.OUTPUT, false));
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
		protected List<? extends Component> getModelChildren() {
			return Collections.emptyList();
		}

	}

}
