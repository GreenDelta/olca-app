package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.core.model.descriptors.RootDescriptor;

public class SetProcessGroupCommand extends Command {

	private final NodeEditPart node;
	private final RootDescriptor process;

	public SetProcessGroupCommand(NodeEditPart node) {
		this.node = node;
		this.process = node.getModel() != null
				? node.getModel().descriptor
				: null;
	}

	@Override
	public boolean canRedo() {
		return false;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public boolean canExecute() {
		return process != null;
	}

	@Override
	public void execute() {
		System.out.println("set group for " + process.name);
	}
}
