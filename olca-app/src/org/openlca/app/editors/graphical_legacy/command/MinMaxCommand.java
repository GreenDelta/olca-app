package org.openlca.app.editors.graphical_legacy.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;

public class MinMaxCommand extends Command {

	private final ProcessNode node;
	private final boolean initiallyMinimized;

	public MinMaxCommand(ProcessNode node) {
		this.node = node;
		initiallyMinimized = node.isMinimized();
	}

	@Override
	public boolean canExecute() {
		return node != null;
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
		node.parent().editor.setDirty();
	}

	@Override
	public String getLabel() {
		if (node.isMinimized()) {
			if (initiallyMinimized)
				return M.Maximize;
			return M.Minimize;
		}
		if (initiallyMinimized)
			return M.Minimize;
		return M.Maximize;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		execute();
	}

}
