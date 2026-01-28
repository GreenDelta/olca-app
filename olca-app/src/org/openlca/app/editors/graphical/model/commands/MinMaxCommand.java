package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.edit.AbstractComponentEditPart;
import org.openlca.app.editors.graphical.model.MinMaxComponent;

public class MinMaxCommand extends Command {

	public static final int MINIMIZE = 1;
	public static final int MAXIMIZE = 2;
	private final int type;
	private MinMaxComponent child;

	public MinMaxCommand(int type) {
		super(type == MINIMIZE ? M.Minimize : M.Maximize);
		this.type = type;
	}

	public void setChild(MinMaxComponent child) {
		this.child = child;
	}

	@Override
	public boolean canExecute() {
		var childType = child.isMinimized() ? MINIMIZE : MAXIMIZE;
		return child != null && type != childType;
	}

	@Override
	public boolean canUndo() {
		var childType = child.isMinimized() ? MINIMIZE : MAXIMIZE;
		return child != null && type == childType;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		// Update model
		child.setMinimized(!child.isMinimized());
		if (!child.isMinimized()) {
			child.addChildren();
		}

		// Note that links are reconnected AFTER creating the children if the
		// command is maximizing and BEFORE if the command is minimizing.
		child.reconnectLinks();
		if (child.isMinimized()) {
			child.removeAllChildren();
		}

		// Update the EditPart.
		var editor = child.getGraph().getEditor();
		var childPart = editor.getEditPartOf(child);
		if (childPart == null) return;
		if (childPart.getParent() instanceof AbstractComponentEditPart<?> parent) {
			parent.resetChildEditPart(childPart);
		}
	}

	public void undo() {
		execute();
	}
}
