package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class CreateProcessCommand extends Command {

	private final ProductSystemNode model;
	private final ProcessDescriptor process;

	public CreateProcessCommand(ProductSystemNode model, ProcessDescriptor process) {
		this.model = model;
		this.process = process;
	}

	@Override
	public boolean canExecute() {
		if (model == null)
			return false;
		return !model.getProductSystem().processes.contains(process.id);
	}

	@Override
	public boolean canUndo() {
		if (model == null)
			return false;
		return model.getProductSystem().processes.contains(process.id);
	}

	@Override
	public void execute() {
		model.getProductSystem().processes.add(process.id);
		model.add(new ProcessNode(process));
		if (model.editor.getOutline() != null)
			model.editor.getOutline().refresh();
		model.editor.setDirty(true);
	}

	@Override
	public String getLabel() {
		return M.CreateProcess;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		model.getProductSystem().processes.remove(process.id);
		model.remove(model.getProcessNode(process.id));
		if (model.editor.getOutline() != null)
			model.editor.getOutline().refresh();
		model.editor.setDirty(true);
	}
}