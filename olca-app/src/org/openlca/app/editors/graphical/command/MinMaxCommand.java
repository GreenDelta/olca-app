package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.IONode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProcessPart;
import org.openlca.app.editors.graphical.model.ProductSystemPart;

public class MinMaxCommand extends Command {

	private final ProductSystemPart productSystemPart;
	private final ProcessPart processPart;
	private final ProcessNode node;
	private final boolean initiallyMinimized;

	public MinMaxCommand(ProductSystemPart productSystemPart, ProcessPart processPart) {
		this.productSystemPart = productSystemPart;
		this.processPart = processPart;
		this.node = processPart.getModel();
		initiallyMinimized = node.isMinimized();
	}

	@Override
	public boolean canExecute() {
		return node != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (node.isMinimized()) {
			node.setIsMinimized(false);
			productSystemPart.resetChildEditPart(processPart);
			if (node.getChildren().isEmpty()) {
				node.add(new IONode(node));
			}
			node.refresh();
		}
		else {
			node.setIsMinimized(true);
			productSystemPart.resetChildEditPart(processPart);
			if (!node.getChildren().isEmpty()) {
				node.remove(node.getChildren().get(0));
			}
			node.refresh();
		}
		productSystemPart.getModel().editor.setDirty();
	}

	@Override
	public String getLabel() {
		if (node.isMinimized()) {
			if (initiallyMinimized)
				return M.Maximize;
			return M.Minimize;
		}
		if (initiallyMinimized)
			return M.Minimize;
		return M.Maximize;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		execute();
	}

}
