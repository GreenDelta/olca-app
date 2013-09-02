package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class ChangeStateCommand extends Command {

	private ProcessNode node;

	ChangeStateCommand() {

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
		if (node.isMinimized())
			node.maximize();
		else
			node.minimize();
	}

	@Override
	public String getLabel() {
		if (node.isMinimized())
			return Messages.Systems_MaximizeCommand_Text;
		else
			return Messages.Systems_MinimizeCommand_Text;
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
