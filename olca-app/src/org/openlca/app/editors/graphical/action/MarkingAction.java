package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.MarkingCommand;
import org.openlca.app.editors.graphical.model.ProcessNode;

class MarkingAction extends EditorAction {

	static final int MARK = 1;
	static final int UNMARK = 2;
	private List<ProcessNode> processNodes = new ArrayList<>();
	private int type;

	MarkingAction(int type) {
		if (type == MARK) {
			setId(ActionIds.MARK);
			setText(Messages.Mark);
		} else if (type == UNMARK) {
			setId(ActionIds.UNMARK);
			setText(Messages.Unmark);
		}
		this.type = type;
	}

	@Override
	public void run() {
		Command actualCommand = null;
		for (ProcessNode node : processNodes) {
			boolean mark = type == MARK;
			if (node.isMarked() != mark) {
				MarkingCommand newCommand = CommandFactory
						.createMarkingCommand(node);
				if (actualCommand == null)
					actualCommand = newCommand;
				else
					actualCommand = actualCommand.chain(newCommand);
			}
		}
		if (actualCommand != null) {
			getEditor().getCommandStack().execute(actualCommand);
			getEditor().selectionChanged(getEditor().getSite().getPart(),
					getEditor().getSelection());
		}
	}

	@Override
	protected boolean accept(ISelection selection) {
		List<ProcessNode> unfilteredNodes = getMultiSelectionOfType(selection,
				ProcessNode.class);
		boolean mark = type == MARK;
		processNodes = new ArrayList<>();
		for (ProcessNode node : unfilteredNodes)
			if (node.isMarked() != mark)
				processNodes.add(node);
		return processNodes.size() > 0;
	}
}
