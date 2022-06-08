package org.openlca.app.editors.graphical_legacy.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;

public class XYLayoutCommand extends Command {

	private static final int MOVE = 1;
	private static final int RESIZE = 2;

	private final ProcessNode node;
	private final Rectangle target;
	private final int type;
	private Rectangle oldBox;

	public static XYLayoutCommand move(ProcessNode node, Rectangle target) {
		return new XYLayoutCommand(node, target, MOVE);
	}

	public static XYLayoutCommand resize(ProcessNode node, Rectangle target) {
		return new XYLayoutCommand(node, target, RESIZE);
	}

	private XYLayoutCommand(ProcessNode node, Rectangle target, int type) {
		this.node = node;
		this.target = target;
		this.type = type;
		this.oldBox = node.getBox();
	}

	@Override
	public boolean canExecute() {
		return target != null && oldBox != null;
	}

	@Override
	public boolean canUndo() {
		return oldBox != null;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void execute() {
		oldBox = node.getBox();
		var newBox = newBox();
		if (node.isMinimized() && newBox.height > node.getMinimumHeight()) {
			node.maximize();
		}
		// minimum height/width is different if node is maximized
		if (newBox.height < node.getMinimumHeight())  {
			newBox.height = node.getMinimumHeight();
		}
		if (newBox.width < node.getMinimumWidth())  {
			newBox.width = node.getMinimumWidth();
		}
		node.setBox(newBox);
		node.parent().editor.setDirty();
	}

	private Rectangle newBox() {
		if (type == MOVE) {
			int x = Math.max(target.x, 0);
			int y = Math.max(target.y, 0);
			return new Rectangle(x, y, oldBox.width, oldBox.height);
		} else {
			int width = Math.max(target.width, node.getMinimumWidth());
			int height = Math.max(target.height, node.getMinimumHeight());
			return new Rectangle(oldBox.x, oldBox.y, width, height);
		}
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
		if (oldBox == null)
			return;
		node.setBox(oldBox);
		node.parent().editor.setDirty();
	}

}
