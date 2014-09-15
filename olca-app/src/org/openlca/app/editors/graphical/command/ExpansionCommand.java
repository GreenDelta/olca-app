package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class ExpansionCommand extends Command {

	final static int EXPAND = 1;
	final static int COLLAPSE = 2;
	final static int LEFT = 1;
	final static int RIGHT = 2;

	private ProcessNode node;
	private int side;
	private int type;

	ExpansionCommand(int type, int side) {
		this.side = side;
		this.type = type;
	}

	@Override
	public boolean canExecute() {
		if (side != LEFT && side != RIGHT)
			return false;
		if (type != EXPAND && type != COLLAPSE)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		if (type == EXPAND) {
			if (side == LEFT)
				node.expandLeft();
			else if (side == RIGHT)
				node.expandRight();
		} else if (type == COLLAPSE) {
			if (side == LEFT)
				node.collapseLeft();
			else if (side == RIGHT)
				node.collapseRight();
		}
		node.layout();
		node.getParent().getEditor().setDirty(true);
		node.select();
		node.reveal();
	}

	@Override
	public String getLabel() {
		if (type == EXPAND)
			return Messages.Expand;
		else if (type == COLLAPSE)
			return Messages.Collapse;
		return null;
	}

	void setNode(ProcessNode node) {
		this.node = node;
	}

}
