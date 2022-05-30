package org.openlca.app.editors.graph.model.commands;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.openlca.app.M;
import org.openlca.app.editors.graph.edit.GraphEditPart;
import org.openlca.app.editors.graph.model.Node;

import static org.openlca.app.editors.graph.actions.LayoutAction.REQ_LAYOUT;
import static org.openlca.app.editors.graph.model.Node.Side;
import static org.openlca.app.editors.graph.model.Node.Side.INPUT;
import static org.openlca.app.editors.graph.model.Node.Side.OUTPUT;

public class ExpansionCommand extends Command {

	private final Node node;
	private final Side side;
	private final boolean isExpand;
	private GraphEditPart graphEditPart;

	public ExpansionCommand(Node node, Side side) {
		this.node = node;
		this.side = side;
		this.isExpand = !node.isExpanded(side);
	}

	@Override
	public boolean canExecute() {
		return side == INPUT || side == OUTPUT;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public boolean canRedo() {
		return true;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		if (isExpand) node.expand(side);
		else node.collapse(side);

		// The layout command has to be executed after creating or deleting the
		// nodes, hence cannot be executed within a CompoundCommand.
		var command = graphEditPart.getCommand(new Request(REQ_LAYOUT));
		if (command.canExecute())
			command.execute();

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

	public void setParent(EditPart parent) {
		graphEditPart = (GraphEditPart) parent;
	}
}
