package org.openlca.app.tools.graphics.model.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.tools.graphics.layouts.GraphLayout;
import org.openlca.app.tools.graphics.model.Component;

import static org.openlca.app.tools.graphics.layouts.GraphLayout.DEFAULT_LOCATION;


public class LayoutCommand extends Command {

	private final GraphLayout layoutManager;
	private final Component parent;
	/** Stores the old size and location. */
	public final Map<Component, Point> oldLocations = new HashMap<>();

	/**
	 * Create a command that can reset the location of all the nodes to force a
	 * complete relayout.
	 */
	public LayoutCommand(GraphicalEditPart part) {
		this.layoutManager = (GraphLayout) part.getFigure().getLayoutManager();
		this.parent = (Component) part.getModel();
		setLabel(M.Layout);
	}

	@Override
	public boolean canExecute() {
		return true;
	}

	@Override
	public void execute() {
		for (var child : parent.getChildren()) {
			oldLocations.put(child, child.getLocation());
		}
		redo();
	}

	@Override
	public void redo() {
		layoutManager.clear();
		for (var child : parent.getChildren())
			child.setLocation(DEFAULT_LOCATION);
	}

	@Override
	public void undo() {
		for (var child : parent.getChildren())
			child.setLocation(oldLocations.get(child));
	}

}
