package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class MarkingCommand extends Command {

	private ProcessNode node;

	MarkingCommand() {

	}

	@Override
	public boolean canExecute() {
		if (node == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (node.isMarked())
			node.unmark();
		else
			node.mark();
		node.getParent().getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		if (node.isMarked())
			return Messages.Unmark;
		else
			return Messages.Mark;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		execute();
	}

	void setNode(ProcessNode node) {
		this.node = node;
	}

}
