package org.openlca.app.editors.graphical_legacy.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;

public class ExpansionCommand extends Command {

	private final static int EXPAND = 1;
	private final static int COLLAPSE = 2;
	private final static int LEFT = 1;
	private final static int RIGHT = 2;

	private final ProcessNode node;
	private final int side;
	private final int type;

	public static ExpansionCommand expandLeft(ProcessNode node) {
		return new ExpansionCommand(node, EXPAND, LEFT);
	}

	public static ExpansionCommand expandRight(ProcessNode node) {
		return new ExpansionCommand(node, EXPAND, RIGHT);
	}

	public static ExpansionCommand collapseLeft(ProcessNode node) {
		return new ExpansionCommand(node, COLLAPSE, LEFT);
	}

	public static ExpansionCommand collapseRight(ProcessNode node) {
		return new ExpansionCommand(node, COLLAPSE, RIGHT);
	}

	private ExpansionCommand(ProcessNode node, int type, int side) {
		this.node = node;
		this.side = side;
		this.type = type;
	}

	@Override
	public boolean canExecute() {
		if (side != LEFT && side != RIGHT)
			return false;
		return type == EXPAND || type == COLLAPSE;
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
		node.parent().editor.setDirty();
		node.select();
		node.reveal();
	}

	@Override
	public String getLabel() {
		if (type == EXPAND)
			return M.Expand;
		else if (type == COLLAPSE)
			return M.Collapse;
		return null;
	}

}
