package org.openlca.app.tools.graphics.layouts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.tools.graphics.figures.ComponentFigure;

import static org.eclipse.draw2d.PositionConstants.*;

public class StackLayout {

	private final double DISTANCE_LEVEL = 48;

	private final List<ComponentFigure> figures = new ArrayList<>();
	private final ComponentFigure rootFigure;
	private final GraphLayout manager;
	private final int direction;

	StackLayout(GraphLayout manager, List<ComponentFigure> stackFigures,
		ComponentFigure rootFigure, int direction) {
		this.manager = manager;
		this.rootFigure = rootFigure;
		this.direction = direction;
		figures.add(rootFigure);
		figures.addAll(stackFigures);
	}

	public void run() {

		var maxDepth = figures.size();

		var levels = new ArrayList<>(Collections.nCopies(maxDepth, 0.0));
		var rootLocation = manager.mapNodeToVertex
				.get(rootFigure.getComponent())
				.endLocation;

		for (int i = 1; i < maxDepth; ++i) {
			levels.set(i, levels.get(i - 1)
					+ ((lengthOfFigure(i - 1) + lengthOfFigure(i)) / 2
					+ DISTANCE_LEVEL)
					* (((direction & SOUTH_EAST) != 0 ) ? 1 : -1));
			var size = manager.getConstrainedSize(figures.get(i));
			var node = figures.get(i).getComponent();
			var vertex = new Vertex(node, figures.get(i), size, i);

			var x = (direction & (NORTH | SOUTH)) != 0
					? rootLocation.x
					: (int) Math.round(rootLocation.x + levels.get(i));
			var y = (direction & (NORTH | SOUTH)) != 0
					? (int) Math.round(rootLocation.y + levels.get(i))
					: rootLocation.y;
			vertex.setLocation(new Point(x, y),
					manager.mapNodeToVertex.get(manager.getReferenceNode()));

			manager.mapNodeToVertex.put(figures.get(i).getComponent(), vertex);
		}
	}

	private int lengthOfFigure(int index) {
		var size = manager.getConstrainedSize(figures.get(index));
		return (direction & (NORTH | SOUTH)) != 0
				? size.height()
				: size.width();
	}

}
