package org.openlca.app.results.analysis.sankey.layout;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.results.analysis.sankey.model.ProcessFigure;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;

public class XYLayoutCommand extends Command {

	private Rectangle layout;
	private Rectangle oldLayout;
	private ProcessNode processNode;

	@Override
	public boolean canExecute() {
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	private Point applyGrid(final int x, final int y) {
		int newX = x;
		int newY = y;

		final int hs = GraphLayoutManager.horizontalSpacing;
		final int vs = GraphLayoutManager.verticalSpacing;
		final int gridWidth = ProcessFigure.WIDTH + hs;
		final int gridHeight = ProcessFigure.HEIGHT + vs;

		if (newX <= hs) {
			newX = hs;
		} else {
			final int xMod = (newX - hs) % gridWidth;

			if (xMod != 0) {
				if (xMod <= gridWidth / 2) {
					newX = x - xMod;
				} else {
					newX = x - xMod + gridWidth;
				}
			}
		}

		if (newY <= vs) {
			newY = vs;
		} else {
			final int yMod = (newY - vs) % gridHeight;

			if (yMod != 0) {
				if (yMod <= gridHeight / 2) {
					newY = y - yMod;
				} else {
					newY = y - yMod + gridHeight;
				}
			}
		}

		return new Point(newX, newY);
	}

	@Override
	public void execute() {
		oldLayout = processNode.getXyLayoutConstraints();

		final Point newLocation = applyGrid(layout.x, layout.y);

		processNode.setXyLayoutConstraints(new Rectangle(newLocation.x,
				newLocation.y, oldLayout.width, oldLayout.height));
	}

	@Override
	public String getLabel() {
		return Messages.Move;
	}

	@Override
	public void redo() {
		execute();
	}

	public void setConstraint(final Rectangle rect) {
		layout = rect;
	}

	public void setProcessNode(final ProcessNode processNode) {
		this.processNode = processNode;
	}

	@Override
	public void undo() {
		processNode.setXyLayoutConstraints(oldLayout);
	}
}
