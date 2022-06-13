package org.openlca.app.editors.graphical_legacy.command;

import java.util.ArrayList;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.core.model.ProductSystem;

public class DeleteProcessCommand extends Command {

	private final ProcessNode node;
	private Rectangle oldLayout;

	/** We also delete links when necessary */
	private DeleteLinkCommand linkCommand;

	public DeleteProcessCommand(ProcessNode node) {
		this.node = node;
	}

	@Override
	public boolean canExecute() {
		if (node == null)
			return false;
		long refID = node.parent().getProductSystem().referenceProcess.id;
		return node.process.id != refID;
	}

	@Override
	public void execute() {
		node.expandLeft();
		node.expandRight();
		if (node.links.size() > 0) {
			linkCommand = new DeleteLinkCommand(
					new ArrayList<>(node.links));
			linkCommand.execute();
		}

		ProductSystemNode sysNode = node.parent();
		ProductSystem system = sysNode.getProductSystem();
		oldLayout = node.getBox();
		system.processes.remove(node.process.id);
		sysNode.remove(node);
		if (sysNode.editor.getOutline() != null) {
			sysNode.editor.getOutline().refresh();
		}
		sysNode.editor.setDirty();
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
		ProductSystemNode sysNode = node.parent();
		sysNode.add(node);
		node.setBox(oldLayout);
		sysNode.getProductSystem().processes.add(node.process.id);
		if (sysNode.editor.getOutline() != null) {
			sysNode.editor.getOutline().refresh();
		}
		if (linkCommand != null) {
			linkCommand.undo();
			linkCommand = null;
		}
		node.parent().editor.setDirty();
	}
}
