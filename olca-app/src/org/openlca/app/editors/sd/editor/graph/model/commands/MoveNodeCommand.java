package org.openlca.app.editors.sd.editor.graph.model.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;

/**
 * Command to move or resize a node.
 */
public class MoveNodeCommand extends Command {

	private final SdNode node;
	private final Rectangle newBounds;
	private Point oldLocation;

	public MoveNodeCommand(SdNode node, Rectangle newBounds) {
		this.node = node;
		this.newBounds = newBounds;
		setLabel("Move " + node.getDisplayName());
	}

	@Override
	public boolean canExecute() {
		return node != null && newBounds != null;
	}

	@Override
	public void execute() {
		oldLocation = node.getLocation().getCopy();
		node.setLocation(newBounds.getLocation());
		// Optionally resize
		if (newBounds.width > 0 && newBounds.height > 0) {
			node.setSize(newBounds.getSize());
		}
	}

	@Override
	public void undo() {
		node.setLocation(oldLocation);
	}

	@Override
	public boolean canUndo() {
		return node != null && oldLocation != null;
	}
}
