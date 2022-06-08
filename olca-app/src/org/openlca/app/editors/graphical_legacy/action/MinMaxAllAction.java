package org.openlca.app.editors.graphical_legacy.action;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.command.MinMaxCommand;
import org.openlca.app.editors.graphical_legacy.command.Commands;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.rcp.images.Icon;

class MinMaxAllAction extends EditorAction {

	static final int MINIMIZE = 1;
	static final int MAXIMIZE = 2;
	private final int type;

	MinMaxAllAction(int type) {
		if (type == MINIMIZE) {
			setId(ActionIds.MINIMIZE_ALL);
			setText(M.MinimizeAll);
			setImageDescriptor(Icon.MINIMIZE.descriptor());
		} else if (type == MAXIMIZE) {
			setId(ActionIds.MAXIMIZE_ALL);
			setText(M.MaximizeAll);
			setImageDescriptor(Icon.MAXIMIZE.descriptor());
		}
		this.type = type;
	}

	@Override
	public void run() {
		Command actualCommand = null;
		for (ProcessNode node : editor.getModel().getChildren()) {
			boolean minimize = type == MINIMIZE;
			if (node.isMinimized() == minimize)
				continue;
			MinMaxCommand newCommand = new MinMaxCommand(node);
			actualCommand = Commands.chain(newCommand, actualCommand);
		}
		if (actualCommand == null)
			return;
		editor.getCommandStack().execute(actualCommand);
	}

	@Override
	protected boolean accept(ISelection selection) {
		return true;
	}

}
