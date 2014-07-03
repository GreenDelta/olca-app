package org.openlca.app.editors.graphical.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class LayoutCommand extends Command {

	private ProductSystemNode model;
	private GraphLayoutManager layoutManager;
	private GraphLayoutType type;
	private Map<IFigure, Rectangle> oldConstraints = new HashMap<>();

	LayoutCommand() {
	}

	@Override
	public boolean canExecute() {
		if (type == null)
			return false;
		if (layoutManager == null)
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
	public void execute() {
		for (ProcessNode node : model.getChildren())
			if (node.getFigure().isVisible())
				oldConstraints.put(node.getFigure(), node.getFigure()
						.getBounds().getCopy());
		layoutManager.layout(model.getFigure(), type);
		model.getEditor().setDirty(true);
	}

	@Override
	public String getLabel() {
		return Messages.Layout + ": " + type.getDisplayName();
	}

	@Override
	public void redo() {
		layoutManager.layout(model.getFigure(), type);
		model.getEditor().setDirty(true);
	}

	@Override
	public void undo() {
		for (ProcessNode node : model.getChildren())
			if (oldConstraints.get(node.getFigure()) != null)
				node.setXyLayoutConstraints(oldConstraints.get(node.getFigure()));
		model.getEditor().setDirty(true);
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

	void setLayoutManager(GraphLayoutManager layoutManager) {
		this.layoutManager = layoutManager;
	}

	void setLayoutType(GraphLayoutType type) {
		this.type = type;
	}

}
