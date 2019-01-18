package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class HideShowCommand extends Command {

	private static final int SHOW = 1;
	private static final int HIDE = 2;

	private final ProductSystemNode model;
	private final ProcessDescriptor process;
	private int type;

	public static HideShowCommand show(ProductSystemNode model, ProcessDescriptor process) {
		return new HideShowCommand(model, process, SHOW);
	}

	public static HideShowCommand hide(ProductSystemNode model, ProcessDescriptor process) {
		return new HideShowCommand(model, process, HIDE);
	}

	private HideShowCommand(ProductSystemNode model, ProcessDescriptor process, int type) {
		this.model = model;
		this.process = process;
		this.type = type;
	}

	@Override
	public void execute() {
		ProcessNode node = model.getProcessNode(process.id);
		if (type == SHOW && node == null) {
			node = new ProcessNode(process);
			model.add(node);
			model.editor.createNecessaryLinks(node);
		}
		if (type == HIDE)
			for (Link link : node.links)
				link.setVisible(false);
		node.setVisible(type == SHOW);
		if (type == SHOW)
			node.showLinks();
		node.layout();
		node.parent().editor.setDirty(true);
	}

	@Override
	public boolean canExecute() {
		if (process == null)
			return false;
		if (model == null)
			return false;
		return true;
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
