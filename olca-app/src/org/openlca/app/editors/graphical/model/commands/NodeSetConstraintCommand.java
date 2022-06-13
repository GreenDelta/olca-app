package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Node;

public class NodeSetConstraintCommand extends Command {

	/** Stores the new size and location. */
	private final Rectangle newBounds;
	/** Stores the old size and location. */
	private Rectangle oldBounds;
	/** A request to move/resize an edit part. */
	private final ChangeBoundsRequest request;

	/** Node to manipulate. */
	private final Node node;

	/**
	 * Create a command that can resize and/or move a node.
	 *
	 * @param node
	 *            the node to manipulate
	 * @param req
	 *            the move and resize request
	 * @param newBounds
	 *            the new size and location
	 * @throws IllegalArgumentException
	 *             if any of the parameters is null
	 */
	public NodeSetConstraintCommand(Node node, ChangeBoundsRequest req,
																	Rectangle newBounds) {
		if (node == null || req == null || newBounds == null) {
			throw new IllegalArgumentException();
		}
		this.node = node;
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
		oldBounds = new Rectangle(node.getLocation(), node.getSize());
		redo();
	}

	@Override
	public void redo() {
		node.setSize(newBounds.getSize());
		node.setLocation(newBounds.getLocation());
	}

	@Override
	public void undo() {
		node.setSize(oldBounds.getSize());
		node.setLocation(oldBounds.getLocation());
	}

}
