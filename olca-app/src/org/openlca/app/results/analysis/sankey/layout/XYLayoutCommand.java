package org.openlca.app.results.analysis.sankey.layout;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
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

	private Point applyGrid(int x, int y) {
		int newX = x;
		int newY = y;
		int hs = GraphLayoutManager.horizontalSpacing;
		int vs = GraphLayoutManager.verticalSpacing;
		int gridWidth = ProcessFigure.WIDTH + hs;
		int gridHeight = ProcessFigure.HEIGHT + vs;
		if (newX <= hs) {
			newX = hs;
		} else {
			int xMod = (newX - hs) % gridWidth;
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
			int yMod = (newY - vs) % gridHeight;
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
		Point newLocation = applyGrid(Math.max(layout.x, 0), Math.max(layout.y, 0));
		processNode.setXyLayoutConstraints(new Rectangle(newLocation.x, newLocation.y, oldLayout.width, oldLayout.height));
	}

	@Override
	public String getLabel() {
		return M.Move;
	}

	@Override
	public void redo() {
		execute();
	}

	public void setConstraint(Rectangle rect) {
		layout = rect;
	}

	public void setProcessNode(ProcessNode processNode) {
		this.processNode = processNode;
	}

	@Override
	public void undo() {
		processNode.setXyLayoutConstraints(oldLayout);
	}
}
