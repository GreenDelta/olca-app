package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProcessNode.Side;

public class ExpansionCommand extends Command {

	private final static int EXPAND = 1;
	private final static int COLLAPSE = 2;

	private final ProcessNode node;
	private final Side side;
	private final int type;

	public static ExpansionCommand expandLeft(ProcessNode node) {
		return new ExpansionCommand(node, EXPAND, Side.INPUT);
	}

	public static ExpansionCommand expandRight(ProcessNode node) {
		return new ExpansionCommand(node, EXPAND, Side.OUTPUT);
	}

	public static ExpansionCommand collapseLeft(ProcessNode node) {
		return new ExpansionCommand(node, COLLAPSE, Side.INPUT);
	}

	public static ExpansionCommand collapseRight(ProcessNode node) {
		return new ExpansionCommand(node, COLLAPSE, Side.OUTPUT);
	}

	private ExpansionCommand(ProcessNode node, int type, Side side) {
		this.node = node;
		this.side = side;
		this.type = type;
	}

	@Override
	public boolean canExecute() {
		if (side != Side.INPUT && side != Side.OUTPUT)
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
			node.expand(side);
		} else if (type == COLLAPSE) {
			node.collapse(side);
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
