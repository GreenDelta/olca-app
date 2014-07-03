package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class HideShowCommand extends Command {

	static final int SHOW = 1;
	static final int HIDE = 2;

	private ProcessDescriptor process;
	private ProductSystemNode model;
	private int type;

	HideShowCommand(int type) {
		this.type = type;
	}

	@Override
	public void execute() {
		ProcessNode node = model.getProcessNode(process.getId());
		if (type == SHOW && node == null) {
			node = new ProcessNode(process);
			model.add(node);
			model.getEditor().createNecessaryLinks(node);
		}
		if (type == HIDE)
			for (ConnectionLink link : node.getLinks())
				link.setVisible(false);
		node.setVisible(type == SHOW);
		if (type == SHOW)
			node.showLinks();
		node.layout();
		node.getParent().getEditor().setDirty(true);
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
			return Messages.Show;
		else if (type == HIDE)
			return Messages.Hide;
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

	void setProcess(ProcessDescriptor process) {
		this.process = process;
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

}
