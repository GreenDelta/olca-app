package org.openlca.app.editors.graphical.edit;

import static org.openlca.app.editors.graphical.GraphConfig.*;
import static org.openlca.app.editors.graphical.requests.GraphRequests.*;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.geometry.Translatable;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.SWT;
import org.openlca.app.components.graphics.model.Component;
import org.openlca.app.components.graphics.model.commands.ComponentSetConstraintCommand;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.commands.CreateNodeCommand;
import org.openlca.app.editors.graphical.model.commands.CreateStickyNoteCommand;
import org.openlca.app.editors.graphical.model.commands.EditConfigCommand;
import org.openlca.app.editors.graphical.model.commands.GraphLayoutCommand;
import org.openlca.app.editors.graphical.requests.GraphRequest;
import org.openlca.core.model.descriptors.RootDescriptor;

public class GraphLayoutPolicy extends XYLayoutEditPolicy {

	@Override
	public Command getCommand(Request req) {
		if (req == null || !(req.getType() instanceof String type)) {
			return super.getCommand(req);
		}
		return switch (type) {
			case REQ_LAYOUT -> new GraphLayoutCommand(getHost());
			case REQ_CREATE -> getCreateCommand((GraphRequest) req);
			case REQ_EDIT_CONFIG -> getEditConfigCommand(req);
			default -> super.getCommand(req);
		};
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;
	}

	protected Command getCreateCommand(GraphRequest request) {
		var command = new CompoundCommand();
		command.setDebugLabel("Create in GraphXYLayoutEditPolicy");

		var descriptors = request.getDescriptors();

		if (descriptors == null)
			command.add(createCreateStickyNoteCommand(
				(Rectangle) getConstraintFor(request)));
		else for (var descriptor : descriptors)
			command.add(createCreateNodeCommand(descriptor,
				(Rectangle) getConstraintFor(request)));

		return command.unwrap();
	}

	protected Command createCreateNodeCommand(RootDescriptor descriptor,
		Rectangle constraint) {
		var graph = (Graph) getHost().getModel();
		return new CreateNodeCommand(graph, descriptor, constraint);
	}

	protected Command createCreateStickyNoteCommand(Rectangle constraint) {
		var graph = (Graph) getHost().getModel();
		return new CreateStickyNoteCommand(graph, constraint);
	}

	@Override
	protected Command createChangeConstraintCommand(
		ChangeBoundsRequest request, EditPart child, Object constraint) {
		if (child instanceof VertexEditPart
			&& constraint instanceof Rectangle rect) {
			if (child instanceof NodeEditPart) {
				var nodeConstraint = new Rectangle(rect.x, rect.y, rect.width, SWT.DEFAULT);
				return new ComponentSetConstraintCommand(
					(Component) child.getModel(), request, nodeConstraint);
			} else return new ComponentSetConstraintCommand(
				(Component) child.getModel(), request, rect);
		}
		return super.createChangeConstraintCommand(request, child,
			constraint);
	}

	@Override
	protected EditPolicy createChildEditPolicy(EditPart child) {
		var policy = new ResizableEditPolicy();
		if (child instanceof NodeEditPart)
			policy.setResizeDirections(
				PositionConstants.EAST | PositionConstants.WEST);
		return policy;
	}

	/**
	 * Generates a draw2d constraint for the given <code>GraphRequest</code>.
	 * <p>
	 * If the GraphRequest has a size, is used during size-on-drop creation, a
	 * Rectangle of the request's location and size is passed with the
	 * delegation. Otherwise, a rectangle with the request's location and an
	 * empty size (0,0) is passed over.
	 * <p>
	 * The GraphRequest's location is relative to the Viewer. The location is
	 * made layout-relative by using
	 * {@link #translateFromAbsoluteToLayoutRelative(Translatable)} before
	 * calling {@link #getConstraintFor(Request, GraphicalEditPart, Rectangle)}.
	 *
	 * @param request the GraphRequest
	 * @return a draw2d constraint
	 */
	protected Object getConstraintFor(GraphRequest request) {
		var locationAndSize = (request.getSize() == null
			|| request.getSize().isEmpty())
			? new PrecisionRectangle(request.getLocation(), UNSPECIFIED_SIZE)
			: new PrecisionRectangle(request.getLocation(), request.getSize());

		translateFromAbsoluteToLayoutRelative(locationAndSize);
		return getConstraintFor(request, null, locationAndSize);
	}

	private Command getEditConfigCommand(Request request) {
		var newConfig = (GraphConfig) request.getExtendedData().get(CONFIG_PROP);
		return new EditConfigCommand((Graph) getHost().getModel(), newConfig);
	}

}
