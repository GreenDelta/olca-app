package org.openlca.app.editors.graphical.model.commands;

import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.tools.graphics.model.commands.LayoutCommand;

import static org.openlca.app.tools.graphics.layouts.GraphLayout.DEFAULT_LOCATION;

public class GraphLayoutCommand extends LayoutCommand {

	private final GraphEditor editor;
	private final Graph graph;

	public GraphLayoutCommand(Component parent) {
		super(parent);
		this.graph = ((Graph) parent);
		this.editor = graph.editor;
	}


	@Override
	public void redo() {
		for (var child : graph.getNodes())
			child.setLocation(DEFAULT_LOCATION);
	}

	@Override
	public void undo() {
		for (var child : graph.getNodes())
			child.setLocation(oldLocations.get(child));
	}

}
