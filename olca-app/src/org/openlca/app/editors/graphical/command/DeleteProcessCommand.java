package org.openlca.app.editors.graphical.command;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class DeleteProcessCommand extends Command {

	private final ProcessNode node;
	private Rectangle oldLayout;

	public DeleteProcessCommand(ProcessNode node) {
		this.node = node;
	}

	@Override
	public boolean canExecute() {
		if (node == null)
			return false;
		if (node.parent().getProductSystem().referenceProcess.id == node.process.id)
			return false;
		return node.links.size() == 0;
	}

	@Override
	public void execute() {
		oldLayout = node.getXyLayoutConstraints();
		node.parent().getProductSystem().processes.remove(node.process.id);
		node.parent().remove(node);
		if (node.parent().editor.getOutline() != null)
			node.parent().editor.getOutline().refresh();
		node.parent().editor.setDirty(true);
	}

	@Override
	public String getLabel() {
		return M.DeleteProcess;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		node.parent().add(node);
		node.setXyLayoutConstraints(oldLayout);
		node.parent().getProductSystem().processes.add(node.process.id);
		if (node.parent().editor.getOutline() != null)
			node.parent().editor.getOutline().refresh();
		node.parent().editor.setDirty(true);
	}
}
