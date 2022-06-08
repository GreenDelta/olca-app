package org.openlca.app.editors.graphical_legacy.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.layout.LayoutManager;
import org.openlca.app.editors.graphical_legacy.layout.LayoutType;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;

public class LayoutCommand extends Command {

	private final ProductSystemNode model;
	private final LayoutManager layoutManager;
	private final LayoutType type;
	private final Map<IFigure, Rectangle> oldConstraints = new HashMap<>();

	public LayoutCommand(GraphEditor editor, LayoutType type) {
		this.model = editor.getModel();
		this.layoutManager = (LayoutManager) model.figure.getLayoutManager();
		this.type = type;
	}

	@Override
	public boolean canExecute() {
		if (type == null)
			return false;
		if (layoutManager == null)
			return false;
		return model != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		for (ProcessNode node : model.getChildren()) {
			if (node.figure.isVisible()) {
				oldConstraints.put(
						node.figure, node.figure.getBounds().getCopy());
			}
		}
		layoutManager.layout(model.figure, type);
		model.editor.setDirty();
	}

	@Override
	public String getLabel() {
		return M.Layout + ": " + type.getDisplayName();
	}

	@Override
	public void redo() {
		layoutManager.layout(model.figure, type);
		model.editor.setDirty();
	}

	@Override
	public void undo() {
		for (ProcessNode node : model.getChildren())
			if (oldConstraints.get(node.figure) != null)
				node.setBox(oldConstraints.get(node.figure));
		model.editor.setDirty();
	}

}
