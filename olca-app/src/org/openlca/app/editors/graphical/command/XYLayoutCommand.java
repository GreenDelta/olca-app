package org.openlca.app.editors.graphical.command;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class XYLayoutCommand extends Command {

	private static final int MOVE = 1;
	private static final int RESIZE = 2;

	private final ProcessNode node;
	private final Rectangle newLayout;
	private final int type;
	private Rectangle previousLayout;

	public static XYLayoutCommand move(ProcessNode node, Rectangle newLayout) {
		return new XYLayoutCommand(node, newLayout, MOVE);
	}

	public static XYLayoutCommand resize(ProcessNode node, Rectangle newLayout) {
		return new XYLayoutCommand(node, newLayout, RESIZE);
	}

	private XYLayoutCommand(ProcessNode node, Rectangle newLayout, int type) {
		this.node = node;
		this.newLayout = newLayout;
		this.type = type;
	}

	@Override
	public boolean canExecute() {
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void execute() {
		previousLayout = node.getXyLayoutConstraints();
		Rectangle newConstraints = newContraints();
		if (node.isMinimized() && newConstraints.height > node.getMinimumHeight()) {
			node.maximize();
		}
		// minimum height/width is different if node is maximized
		if (newConstraints.height < node.getMinimumHeight())  {
			newConstraints.height = node.getMinimumHeight();
		}
		if (newConstraints.width < node.getMinimumWidth())  {
			newConstraints.width = node.getMinimumWidth();
		}
		node.setXyLayoutConstraints(newConstraints);
		node.parent().editor.setDirty(true);
	}

	private Rectangle newContraints() {
		Point position = null;
		Dimension size = null;
		if (type == MOVE) {
			position = new Point(Math.max(newLayout.x, 0), Math.max(newLayout.y, 0));
			size = new Dimension(previousLayout.width, previousLayout.height);
		} else if (type == RESIZE) {
			position = new Point(previousLayout.x, previousLayout.y);
			int width = Math.max(newLayout.width, node.getMinimumWidth());
			int height = Math.max(newLayout.height, node.getMinimumHeight());
			size = new Dimension(width, height);
		}
		return new Rectangle(position, size);
	}

	@Override
	public String getLabel() {
		if (type == MOVE)
			return M.Move;
		else if (type == RESIZE)
			return M.Resize;
		return null;
	}

	@Override
	public void undo() {
		node.setXyLayoutConstraints(previousLayout);
		node.parent().editor.setDirty(true);
	}

}
