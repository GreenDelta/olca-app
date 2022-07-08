package org.openlca.app.editors.graphical.model.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;

import static org.openlca.app.editors.graphical.layouts.GraphLayout.DEFAULT_LOCATION;

public class LayoutCommand extends Command {

	private final Graph graph;
	/** Stores the old size and location. */
	private final Map<Node, Point> oldLocations = new HashMap<>();

	/**
	 * Create a command that can reset the location of all the nodes to force a
	 * complete relayout.
	 */
	public LayoutCommand(Graph graph) {
		if (graph == null) {
			throw new IllegalArgumentException();
		}
		this.graph = graph;
		setLabel(M.Layout);
	}

	@Override
	public boolean canExecute() {
		return true;
	}

	@Override
	public void execute() {
		for (var child : graph.getChildren()) {
			oldLocations.put(child, child.getLocation());
		}
		redo();
	}

	@Override
	public void redo() {
		for (var child : graph.getChildren())
			child.setLocation(DEFAULT_LOCATION);
	}

	@Override
	public void undo() {
		for (var child : graph.getChildren()) {
			child.setLocation(oldLocations.get(child));
		}
	}

}
