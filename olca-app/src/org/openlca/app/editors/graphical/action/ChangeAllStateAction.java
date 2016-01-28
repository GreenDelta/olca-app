package org.openlca.app.editors.graphical.action;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.command.ChangeStateCommand;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.rcp.images.Icon;

class ChangeAllStateAction extends Action {

	static final int MINIMIZE = 1;
	static final int MAXIMIZE = 2;

	ChangeAllStateAction(int type) {
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

	private ProductSystemGraphEditor editor;
	private int type;

	@Override
	public void run() {
		Command actualCommand = null;
		for (ProcessNode node : editor.getModel().getChildren()) {
			boolean minimize = type == MINIMIZE;
			if (node.isMinimized() != minimize) {
				ChangeStateCommand newCommand = CommandFactory
						.createChangeStateCommand(node);
				if (actualCommand == null)
					actualCommand = newCommand;
				else
					actualCommand = actualCommand.chain(newCommand);
			}
		}
		if (actualCommand != null)
			editor.getCommandStack().execute(actualCommand);
	}

	void setEditor(ProductSystemGraphEditor editor) {
		this.editor = editor;
	}

}
