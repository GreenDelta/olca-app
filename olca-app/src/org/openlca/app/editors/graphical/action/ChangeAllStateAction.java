package org.openlca.app.editors.graphical.action;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.command.ChangeStateCommand;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.rcp.ImageType;

class ChangeAllStateAction extends Action {

	static final int MINIMIZE = 1;
	static final int MAXIMIZE = 2;

	ChangeAllStateAction(int type) {
		if (type == MINIMIZE) {
			setId(ActionIds.MINIMIZE_ALL);
			setText(Messages.MinimizeAll);
			setImageDescriptor(ImageType.MINIMIZE_ICON.getDescriptor());
		} else if (type == MAXIMIZE) {
			setId(ActionIds.MAXIMIZE_ALL);
			setText(Messages.MaximizeAll);
			setImageDescriptor(ImageType.MAXIMIZE_ICON.getDescriptor());
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
