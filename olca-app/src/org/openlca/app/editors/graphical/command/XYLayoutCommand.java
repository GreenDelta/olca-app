package org.openlca.app.editors.graphical.command;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class XYLayoutCommand extends Command {

	static final int MOVE = 1;
	static final int RESIZE = 2;

	private Rectangle newLayout;
	private Rectangle previousLayout;
	private ProcessNode node;
	private int type;

	XYLayoutCommand(int type) {
		this.type = type;
	}

	@Override
	public boolean canExecute() {
		if (type == RESIZE) {
			if (node.isMinimized())
				if (newLayout.height != node.getXyLayoutConstraints().height)
					return false;
			if (newLayout.getSize().width < node.getMinimumWidth())
				return false;
			if (newLayout.getSize().height < node.getMinimumHeight())
				return false;
			if (!node.isMinimized())
				if (newLayout.getSize().height < node.getMinimumHeight())
					return false;
		}
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
		node.setXyLayoutConstraints(newContraints());
		node.getParent().getEditor().setDirty(true);
	}

	private Rectangle newContraints() {
		Point position = null;
		Dimension size = null;
		if (type == MOVE) {
			position = new Point(newLayout.x, newLayout.y);
			size = new Dimension(previousLayout.width, previousLayout.height);
		} else if (type == RESIZE) {
			position = new Point(previousLayout.x, previousLayout.y);
			size = new Dimension(newLayout.width, newLayout.height);
		}
		return new Rectangle(position, size);
	}

	@Override
	public String getLabel() {
		if (type == MOVE)
			return Messages.Move;
		else if (type == RESIZE)
			return Messages.Resize;
		return null;
	}

	@Override
	public void undo() {
		node.setXyLayoutConstraints(previousLayout);
		node.getParent().getEditor().setDirty(true);
	}

	void setLayout(Rectangle layout) {
		newLayout = layout;
	}

	void setNode(ProcessNode node) {
		this.node = node;
	}

}
