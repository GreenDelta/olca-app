package org.openlca.app.tools.graphics.model.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.tools.graphics.model.Component;

import static org.openlca.app.tools.graphics.layouts.GraphLayout.DEFAULT_LOCATION;


public class LayoutCommand extends Command {

	private final Component parent;
	/** Stores the old size and location. */
	private final Map<Component, Point> oldLocations = new HashMap<>();

	/**
	 * Create a command that can reset the location of all the nodes to force a
	 * complete relayout.
	 */
	public LayoutCommand(Component parent) {
		if (parent == null) {
			throw new IllegalArgumentException();
		}
		this.parent = parent;
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
		for (var child : parent.getChildren())
			child.setLocation(DEFAULT_LOCATION);
	}

	@Override
	public void undo() {
		for (var child : parent.getChildren())
			child.setLocation(oldLocations.get(child));
	}

}
