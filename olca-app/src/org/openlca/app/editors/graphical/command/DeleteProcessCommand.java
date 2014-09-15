package org.openlca.app.editors.graphical.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class DeleteProcessCommand extends Command {

	private Rectangle oldLayout;
	private ProcessNode node;

	DeleteProcessCommand() {

	}

	@Override
	public boolean canExecute() {
		if (node == null)
			return false;
		if (node.getParent().getProductSystem().getReferenceProcess().getId() == node
				.getProcess().getId())
			return false;
		return node.getLinks().size() == 0;
	}

	@Override
	public void execute() {
		oldLayout = node.getXyLayoutConstraints();
		node.getParent().getProductSystem().getProcesses()
				.remove(node.getProcess().getId());
		node.getParent().remove(node);
		if (node.getParent().getEditor().getOutline() != null)
			node.getParent().getEditor().getOutline().refresh();
		node.getParent().getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		return Messages.DeleteProcess;
	}

	@Override
	public void redo() {
		execute();
	}

	void setNode(ProcessNode node) {
		this.node = node;
	}

	@Override
	public void undo() {
		node.getParent().add(node);
		node.setXyLayoutConstraints(oldLayout);
		node.getParent().getProductSystem().getProcesses()
				.add(node.getProcess().getId());
		if (node.getParent().getEditor().getOutline() != null)
			node.getParent().getEditor().getOutline().refresh();
		node.getParent().getEditor().setDirty(true);
	}
}
