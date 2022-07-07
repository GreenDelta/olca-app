package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.edit.AbstractComponentEditPart;
import org.openlca.app.editors.graphical.model.MinMaxGraphComponent;
import org.openlca.app.editors.graphical.model.Node;

public class MinMaxCommand extends Command {

	public static final int MINIMIZE = 1;
	public static final int MAXIMIZE = 2;
	private final int type;
	private MinMaxGraphComponent child;

	public MinMaxCommand(int type) {
		super(type == MINIMIZE ? M.Minimize : M.Maximize);
		this.type = type;
	}

	public void setChild(MinMaxGraphComponent child) {
		this.child = child;
	}

	@Override
	public boolean canExecute() {
		var childType = child.isMinimized() ? MINIMIZE : MAXIMIZE;
		return  child != null && type != childType;
	}

	@Override
	public boolean canUndo() {
		var childType = child.isMinimized() ? MINIMIZE : MAXIMIZE;
		return  child != null && type == childType;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		child.setMinimized(!child.isMinimized());

		if (!child.isMinimized())
			child.addChildren();
		child.updateLinks();
		if (child.isMinimized())
			child.removeChildren();

		var viewer = (GraphicalViewer) child.editor
			.getAdapter(GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();
		var childEditPart = (EditPart) registry.get(child);
		var parentEditPart = (AbstractComponentEditPart<?>) childEditPart
			.getParent();
		parentEditPart.resetChildEditPart(childEditPart);
	}

	public void undo() {
		execute();
	}

}
