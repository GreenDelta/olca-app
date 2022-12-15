package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.GraphicalEditPart;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.tools.graphics.model.commands.LayoutCommand;


public class GraphLayoutCommand extends LayoutCommand {

	private final Graph graph;

	public GraphLayoutCommand(GraphicalEditPart part) {
		super(part);
		graph = (Graph) part.getModel();
	}

	@Override
	public void undo() {
		for (var child : graph.getNodes())
			child.setLocation(oldLocations.get(child));
	}

}
