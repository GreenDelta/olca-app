package org.openlca.app.tools.graphics.model.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.openlca.app.M;
import org.openlca.app.tools.graphics.model.Component;

public class ComponentSetConstraintCommand extends Command {

	/** Stores the new size and location. */
	private final Rectangle newBounds;
	/** Stores the old size and location. */
	private Rectangle oldBounds;
	/** A request to move/resize an edit part. */
	private final ChangeBoundsRequest request;

	/** Node to manipulate. */
	private final Component component;

	/**
	 * Create a command that can resize and/or move a node.
	 *
	 * @param component
	 *            the node to manipulate
	 * @param req
	 *            the move and resize request
	 * @param newBounds
	 *            the new size and location
	 * @throws IllegalArgumentException
	 *             if any of the parameters is null
	 */
	public ComponentSetConstraintCommand(Component component, ChangeBoundsRequest req,
																			 Rectangle newBounds) {
		if (component == null || req == null || newBounds == null) {
			throw new IllegalArgumentException();
		}
		this.component = component;
		this.request = req;
		this.newBounds = newBounds.getCopy();

		Object type = request.getType();
		if (RequestConstants.REQ_RESIZE_CHILDREN.equals(type)
		|| RequestConstants.REQ_RESIZE.equals(type))
			setLabel(M.Resize);
		else if (RequestConstants.REQ_MOVE_CHILDREN.equals(type)
		|| RequestConstants.REQ_MOVE.equals(type))
			setLabel(M.Move);
		else setLabel("");
	}

	@Override
	public boolean canExecute() {
		Object type = request.getType();
		return (RequestConstants.REQ_MOVE.equals(type)
			|| RequestConstants.REQ_MOVE_CHILDREN.equals(type)
			|| RequestConstants.REQ_RESIZE.equals(type)
			|| RequestConstants.REQ_RESIZE_CHILDREN.equals(type));
	}

	@Override
	public void execute() {
		oldBounds = new Rectangle(component.getLocation(), component.getSize());
		redo();
	}

	@Override
	public void redo() {
		component.setSize(newBounds.getSize());
		component.setLocation(newBounds.getLocation());
	}

	@Override
	public void undo() {
		component.setSize(oldBounds.getSize());
		component.setLocation(oldBounds.getLocation());
	}

}
