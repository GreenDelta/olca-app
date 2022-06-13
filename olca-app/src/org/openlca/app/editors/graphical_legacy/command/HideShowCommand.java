package org.openlca.app.editors.graphical_legacy.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.model.Link;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.core.model.descriptors.RootDescriptor;

public class HideShowCommand extends Command {

	private static final int SHOW = 1;
	private static final int HIDE = 2;

	private final ProductSystemNode model;
	private final RootDescriptor process;
	private int type;

	public static HideShowCommand show(
			ProductSystemNode model, RootDescriptor process) {
		return new HideShowCommand(model, process, SHOW);
	}

	public static HideShowCommand hide(
			ProductSystemNode model, RootDescriptor process) {
		return new HideShowCommand(model, process, HIDE);
	}

	private HideShowCommand(
			ProductSystemNode model,
			RootDescriptor process, int type) {
		this.model = model;
		this.process = process;
		this.type = type;
	}

	@Override
	public void execute() {
		ProcessNode node = model.getProcessNode(process.id);
		if (type == SHOW && node == null) {
			node = new ProcessNode(model.editor, process);
			model.add(node);
			model.editor.createNecessaryLinks(node);
		}
		node.setVisible(type == SHOW);
		for (Link link : node.links) {
			link.updateVisibility();
		}
		node.layout();
		node.parent().editor.setDirty();
	}

	@Override
	public boolean canExecute() {
		if (process == null)
			return false;
		return model != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public String getLabel() {
		if (type == SHOW)
			return M.Show;
		else if (type == HIDE)
			return M.Hide;
		return null;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		switchType();
		execute();
		switchType();
	}

	private void switchType() {
		if (type == SHOW)
			type = HIDE;
		else if (type == HIDE)
			type = SHOW;
	}

}
