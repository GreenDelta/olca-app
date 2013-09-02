package org.openlca.app.editors.graphical;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.CreateProcessCommand;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class GraphDropListener extends DropTargetAdapter {

	private ProductSystemNode model;
	private CommandStack commandStack;
	private Transfer transferType;

	GraphDropListener(ProductSystemNode model, Transfer transferType,
			CommandStack commandStack) {
		this.model = model;
		this.transferType = transferType;
		this.commandStack = commandStack;
	}

	@Override
	public void drop(DropTargetEvent event) {
		boolean valid = validateInput(event);
		if (!valid)
			return;
		Object[] data = (Object[]) event.data;
		ProcessDescriptor[] descriptors = new ProcessDescriptor[data.length];
		for (int i = 0; i < data.length; i++)
			descriptors[i] = (ProcessDescriptor) data[i];

		Command command = null;
		for (ProcessDescriptor process : descriptors) {
			CreateProcessCommand cmd = CommandFactory
					.createCreateProcessCommand(model, process);
			if (command == null)
				command = cmd;
			else
				command = command.chain(cmd);
		}
		if (command != null && command.canExecute())
			commandStack.execute(command);
	}

	private boolean validateInput(DropTargetEvent event) {
		if (!transferType.isSupportedType(event.currentDataType))
			return false;
		if (!(event.data instanceof Object[]))
			return false;
		Object[] data = (Object[]) event.data;
		if (data.length == 0)
			return false;
		for (Object obj : data)
			if (!(obj instanceof ProcessDescriptor))
				return false;
		return true;
	}

}
