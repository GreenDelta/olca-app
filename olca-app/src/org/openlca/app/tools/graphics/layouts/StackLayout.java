package org.openlca.app.tools.graphics.layouts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.tools.graphics.figures.ComponentFigure;

public class StackLayout {

	private final double DISTANCE_LEVEL = 48;

	private final List<ComponentFigure> figures = new ArrayList<>();
	private final ComponentFigure rootFigure;
	private final GraphLayout manager;

	StackLayout(GraphLayout manager, List<ComponentFigure> stackFigures,
		ComponentFigure rootFigure) {
		this.manager = manager;
		this.rootFigure = rootFigure;
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
					+ (heightOfFigure(i - 1) + heightOfFigure(i)) / 2
					+ DISTANCE_LEVEL);
			var size = manager.getConstrainedSize(figures.get(i));
			var node = figures.get(i).getComponent();
			var vertex = new Vertex(node, figures.get(i), size, i);
			var x = rootLocation.x;
			var y = (int) Math.round(rootLocation.y + levels.get(i));
			vertex.setLocation(new Point(x, y),
					manager.mapNodeToVertex.get(manager.getReferenceNode()));
			manager.mapNodeToVertex.put(figures.get(i).getComponent(), vertex);
		}
	}

	private int heightOfFigure(int index) {
		return manager.getConstrainedSize(figures.get(index)).height();
	}

}
