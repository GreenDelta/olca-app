package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;

class SystemMoveCmd extends Command {

	private final SystemNode node;
	private final Rectangle oldBounds;
	private final Rectangle newBounds;

	SystemMoveCmd(SystemNode node, Rectangle newBounds) {
		this.node = node;
		this.oldBounds = node.bounds().getCopy();
		this.newBounds = newBounds.getCopy();
	}

	@Override
	public void execute() {
		node.moveTo(newBounds);
	}

	@Override
	public void undo() {
		node.moveTo(oldBounds);
	}
}
