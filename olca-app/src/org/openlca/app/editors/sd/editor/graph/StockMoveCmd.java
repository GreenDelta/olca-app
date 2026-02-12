package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;

class StockMoveCmd extends Command {

	private final StockModel model;
	private final Rectangle oldBounds;
	private final Rectangle newBounds;

	StockMoveCmd(StockModel model, Rectangle newBounds) {
		this.model = model;
		this.oldBounds = model.bounds.getCopy();
		this.newBounds = newBounds.getCopy();
	}

	@Override
	public void execute() {
		model.moveTo(newBounds);
	}

	@Override
	public void undo() {
		model.moveTo(oldBounds);
	}

}
