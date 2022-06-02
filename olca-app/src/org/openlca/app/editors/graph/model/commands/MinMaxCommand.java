package org.openlca.app.editors.graph.model.commands;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graph.edit.AbstractComponentEditPart;
import org.openlca.app.editors.graph.model.GraphFactory;
import org.openlca.app.editors.graph.model.MinMaxGraphComponent;

public class MinMaxCommand extends Command {

	private final boolean minimize;
	private MinMaxGraphComponent child;
	private EditPart childEditPart;

	public MinMaxCommand(boolean minimize) {
		super(minimize ? M.Minimize : M.Maximize);
		this.minimize = minimize;
	}

	public void setChild(MinMaxGraphComponent child) {
		this.child = child;
	}

	public void setChildEditPart(EditPart childEditPart) {
		this.childEditPart = childEditPart;
	}

	@Override
	public boolean canExecute() {
		return  child != null && childEditPart != null
			&& minimize != child.isMinimized();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		child.setMinimized(minimize);
		if (!minimize)
			child.addChildren();
		child.updateLinks();
		if (minimize)
			child.removeChildren();
		var parentEditPart = (AbstractComponentEditPart<?>) childEditPart.getParent();
		parentEditPart.resetChildEditPart(childEditPart);
	}

	public void undo() {
		execute();
	}

}
