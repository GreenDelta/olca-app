package org.openlca.app.editors.graphical.edit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graphical.figures.MaximizedStickyNoteFigure;
import org.openlca.app.editors.graphical.figures.MinimizedStickyNoteFigure;
import org.openlca.app.editors.graphical.figures.NodeFigure;
import org.openlca.app.editors.graphical.figures.StickyNoteFigure;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.editors.graphical.model.commands.DeleteStickyNoteCommand;
import org.openlca.app.editors.graphical.requests.ExpandCollapseRequest;
import org.openlca.app.tools.graphics.model.Component;

import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.List;

import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public abstract class StickyNoteEditPart extends
		AbstractVertexEditPart<StickyNote> {

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.COMPONENT_ROLE,
				new MinMaxComponentEditPolicy());
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

	protected void addButtonActionListener(StickyNoteFigure figure) {
		figure.closeButton.addActionListener($ -> {
			var command = getCommand(new GroupRequest(REQ_DELETE));
			if (command.canExecute())
				getViewer().getEditDomain().getCommandStack().execute(command);
		});
	}

	@Override
	public StickyNoteFigure getFigure() {
		return (StickyNoteFigure) super.getFigure();
	}

	@Override
	public String toString() {
		return "EditPart of " + getModel();
	}

	public static class Maximized extends StickyNoteEditPart {

		@Override
		protected IFigure createFigure() {
			var figure = new MaximizedStickyNoteFigure(getModel());
			addButtonActionListener(figure);
			return figure;
		}

	}

	public static class Minimized extends StickyNoteEditPart {

		@Override
		protected IFigure createFigure() {
			return new MinimizedStickyNoteFigure(getModel());
		}

		@Override
		protected List<? extends Component> getModelChildren() {
			return Collections.emptyList();
		}

	}

}
