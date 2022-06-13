package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Node;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;

public class ExpansionCommand extends Command {

	private final Node node;
	private final int side;
	private final boolean isExpand;

	public ExpansionCommand(Node node, int side,
													boolean isExpand) {
		this.node = node;
		this.side = side;
		this.isExpand = isExpand;
	}


	@Override
	public boolean canExecute() {
		return node.isExpanded(side) != isExpand
			&& (side == INPUT || side == OUTPUT);
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		if (isExpand) node.expand(side);
		else node.collapse(side);

		node.editor.setDirty();
	}

	@Override
	public void undo() {
		// TODO (francois) ExpansionCommand.undo.
	}

	@Override
	public String getLabel() {
		if (isExpand)
			return M.Expand;
		else return M.Collapse;
	}

}
