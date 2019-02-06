package org.openlca.app.editors.graphical;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.command.CreateProcessCommand;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class GraphDropListener extends DropTargetAdapter {

	private ProductSystemNode model;
	private CommandStack commandStack;
	private Transfer transferType;

	GraphDropListener(ProductSystemNode model,
			Transfer transfer, CommandStack commands) {
		this.model = model;
		this.transferType = transfer;
		this.commandStack = commands;
	}

	@Override
	public void drop(DropTargetEvent e) {
		if (!transferType.isSupportedType(e.currentDataType))
			return;
		if (!(e.data instanceof Object[]))
			return;
		Object[] data = (Object[]) e.data;

		Command command = null;
		for (Object obj : data) {
			if (!(obj instanceof CategorizedDescriptor))
				continue;
			CategorizedDescriptor d = (CategorizedDescriptor) obj;
			if (d.type != ModelType.PRODUCT_SYSTEM
					&& d.type != ModelType.PROCESS)
				continue;
			Command c = new CreateProcessCommand(model, d);
			command = CommandUtil.chain(c, command);
		}
		if (command == null || !command.canExecute())
			return;
		commandStack.execute(command);
	}
}
