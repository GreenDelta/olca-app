package org.openlca.app.editors.graphical.layouts;

import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.figures.NodeFigure;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.search.LinkSearchMap;
import org.openlca.core.model.ProcessLink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A layout for {@link org.eclipse.draw2d.FreeformFigure FreeformFigures} of
 * Graph.
 * This layout intends to lay out the NodeFigure in a tree fashion if the model
 * location is not DEFAULT_LOCATION. If the location is not default, it means
 * that the figure has been moved by the user. The figure is thus laid out as a
 * classical XYLayout figure.
 */
public class GraphLayout extends FreeformLayout {

	/** Integer.MAX_VALUE is used as a default value with the limited risk of
	 * having a figure in that area of the canvas.
	 */
	public static final Point DEFAULT_LOCATION =
		new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);

	private final GraphEditPart graphEditPart;
	private final Graph graph;
	public final TreeLayoutProcessor layoutProcessor = new TreeLayoutProcessor();

	private Map<NodeFigure, Point> mapFigureToLocation;

	public GraphLayout(GraphEditPart graphEditPart) {
		this.graphEditPart = graphEditPart;
		this.graph = graphEditPart.getModel();
	}

	@Override
	public void layout(IFigure parent) {
		Point offset = getOrigin(parent);

		for (var child : parent.getChildren()) {
			if (child instanceof NodeFigure figure) {
				var constraint = (Rectangle) getConstraint(figure);
				if (constraint == null)
					continue;

				var size = calculateSize(figure);

				// Set the bounds to the TreeLayout position if the figure has not been
				// moved manually.
				var bounds = constraint.getLocation().x == Integer.MAX_VALUE
					        || constraint.getLocation().y == Integer.MAX_VALUE
					? new Rectangle(calculatePreferredLocation(figure), size)
					: new Rectangle(constraint.getLocation().getCopy(), size);

				figure.setBounds(bounds.getTranslated(offset));
			}
		}
	}

	protected Dimension calculateSize(IFigure figure) {
		var constraint = (Rectangle) getConstraint(figure);
		var size = new Dimension(constraint.getSize().getCopy());

		if (constraint.width == SWT.DEFAULT
			|| constraint.height == SWT.DEFAULT) {
			Dimension preferredSize = figure.getPreferredSize(constraint.width,
				constraint.height);
			if (constraint.width == SWT.DEFAULT)
				size.width = preferredSize.width;
			if (constraint.height == SWT.DEFAULT)
				size.height = preferredSize.height;
		}
		return size;
	}

	private Point calculatePreferredLocation(NodeFigure figure) {
		if (mapFigureToLocation == null
			|| mapFigureToLocation.isEmpty()
		  || mapFigureToLocation.get(figure) == null ) {
			mapFigureToLocation = layoutProcessor.getNodeLocations();
		}
		return mapFigureToLocation.get(figure);
	}

	public class TreeLayoutProcessor {

		public static int H_SPACE = 25;
		public static int V_SPACE = 25;

		private final Map<Long, Integer> containing = new HashMap<>();
		private final Map<Integer, Integer> heights = new HashMap<>();
		private final Map<Integer, Integer> widths = new HashMap<>();
		private final Map<Point, Long> locations = new HashMap<>();
		private LinkSearchMap linkSearch;

		private void applyLayout(Vertex vertex, int addition, int actualDepth) {
			int x = actualDepth;
			int y = vertex.getSize() / 2 + addition;
			while (locations.get(new Point(x, y)) != null) {
				y++;
				addition++;
			}
			locations.put(new Point(x, y), vertex.processId);
			for (int i = 0; i < vertex.leftChildren.size(); i++) {
				Vertex child = vertex.leftChildren.get(i);
				int sizeAddition = 0;
				for (int j = 0; j < i; j++)
					sizeAddition += vertex.leftChildren.get(j).getSize();
				applyLayout(child, addition + sizeAddition, actualDepth - 1);
			}
			for (int i = 0; i < vertex.rightChildren.size(); i++) {
				Vertex child = vertex.rightChildren.get(i);
				int sizeAddition = 0;
				for (int a = 0; a < vertex.leftChildren.size(); a++)
					sizeAddition += vertex.leftChildren.get(a).getSize();
				for (int j = 0; j < i; j++)
					sizeAddition += vertex.rightChildren.get(j).getSize();
				applyLayout(child, addition + sizeAddition, actualDepth + 1);
			}
		}

		private Vertex build() {
			Vertex vertex = new Vertex();
			vertex.processId = graph.getProductSystem().referenceProcess.id;
			build(new Vertex[] { vertex });
			return vertex;
		}

		private void build(Vertex[] vertices) {
			List<Vertex> children = new ArrayList<>();
			for (Vertex vertex : vertices) {
				long processId = vertex.processId;
				for (ProcessLink link : linkSearch.getLinks(processId)) {
					if (link.processId != processId)
						continue;
					long providerId = link.providerId;
					if (containing.get(providerId) != null)
						continue;
					Vertex child = new Vertex();
					child.processId = providerId;
					vertex.leftChildren.add(child);
					containing.put(child.processId, 1);
					children.add(child);
				}
			}
			if (children.size() > 0)
				build(children.toArray(new Vertex[0]));
			children.clear();
			for (Vertex vertex : vertices) {
				long processId = vertex.processId;
				for (ProcessLink link : linkSearch.getLinks(processId)) {
					if (link.providerId != processId)
						continue;
					long recipientId = link.processId;
					if (containing.get(recipientId) != null)
						continue;
					Vertex child = new Vertex();
					child.processId = recipientId;
					vertex.rightChildren.add(child);
					containing.put(child.processId, 1);
					children.add(child);
				}
			}
			if (children.size() > 0)
				build(children.toArray(new Vertex[0]));
		}

		public Map<NodeFigure, Point> getNodeLocations() {
			this.linkSearch = graph.linkSearch;

			Map<NodeFigure, Point> mapNodeFigureToLocation = new HashMap<>();
			List<Vertex> vertices = new ArrayList<>();

			Vertex mainVertex = build();
			mainVertex.sort();
			vertices.add(mainVertex);
			for (Node node : graph.getChildren()) {
				if (containing.get(node.descriptor.id) != null)
					continue;
				var vertex = new Vertex();
				vertex.processId = node.descriptor.id;
				build(new Vertex[] { vertex });
				vertex.sort();
				vertices.add(vertex);
			}

			int additionalHeight = 0;
			for (Vertex vertex : vertices) {
				int newAdditionalHeight = 0;
				locations.clear();
				applyLayout(vertex, 0, vertex.getLeftDepth());
				int minimumX = Integer.MAX_VALUE;
				int maximumX = Integer.MIN_VALUE;
				int minimumY = Integer.MAX_VALUE;
				int maximumY = Integer.MIN_VALUE;
				for (Point p : locations.keySet()) {
					if (p.x < minimumX)
						minimumX = p.x;
					if (p.x > maximumX)
						maximumX = p.x;
					if (p.y < minimumY)
						minimumY = p.y;
					if (p.y > maximumY)
						maximumY = p.y;
				}

				Map<Long, NodeFigure> mapIDToNodeFigure = new HashMap<>();
				for (var nodeEditPart : graphEditPart.getChildren()) {
					var figure = nodeEditPart.getFigure();
					mapIDToNodeFigure.put(figure.node.descriptor.id, figure);
				}

				for (int x = minimumX; x <= maximumX; x++) {
					widths.put(x, 0);
					for (int y = minimumY; y <= maximumY; y++) {
						Long processId = locations.get(new Point(x, y));
						if (processId == null)
							continue;
						var figure = mapIDToNodeFigure.get(processId);
						if (figure == null)
							continue;
						Dimension size = calculateSize(figure);
						if (size == null)
							continue;
						int width = Math.max(widths.get(x), size.width);
						widths.put(x, width);
					}
				}
				for (int y = minimumY; y <= maximumY; y++) {
					heights.put(y, 0);
					for (int x = minimumX; x <= maximumX; x++) {
						Long processId = locations.get(new Point(x, y));
						if (processId == null)
							continue;
						var figure = mapIDToNodeFigure.get(processId);
						if (figure == null)
							continue;
						Dimension size = calculateSize(figure);
						if (size == null)
							continue;
						int height = Math.max(heights.get(y), size.height);
						heights.put(y, height);
					}
				}

				int xPosition = H_SPACE;
				for (int x = minimumX; x <= maximumX; x++) {
					if (x > minimumX && widths.get(x - 1) > 0)
						xPosition += widths.get(x - 1) + H_SPACE;
					int yPosition = V_SPACE;
					for (int y = minimumY; y <= maximumY; y++) {
						Long processId = locations.get(new Point(x, y));
						if (y > minimumY && heights.get(y - 1) > 0)
							yPosition += heights.get(y - 1) + V_SPACE;
						if (processId == null)
							continue;
						var figure = mapIDToNodeFigure.get(processId);
						if (figure == null)
							continue;
						var newLoc = new Point(xPosition, yPosition + additionalHeight);
						mapNodeFigureToLocation.put(figure, newLoc);
						var totalHeight = yPosition + additionalHeight
							+ calculateSize(figure).height;
						newAdditionalHeight = Math.max(newAdditionalHeight, totalHeight);
					}
				}
				additionalHeight = newAdditionalHeight + V_SPACE;
			}
			containing.clear();
			widths.clear();
			heights.clear();
			locations.clear();
			return mapNodeFigureToLocation;
		}

		private static class Vertex {

			long processId;
			public NodeFigure figure;
			List<Vertex> leftChildren = new ArrayList<>();
			List<Vertex> rightChildren = new ArrayList<>();

			int getLeftDepth() {
				if (leftChildren.size() == 0)
					return 0;
				int depth = 1;
				int depthAdd = 0;
				for (Vertex leftChild : leftChildren)
					depthAdd = Math.max(depthAdd, leftChild.getLeftDepth());
				depth += depthAdd;
				return depth;
			}

			int getSize() {
				if (rightChildren.size() == 0 && leftChildren.size() == 0)
					return 1;
				int size = 0;
				for (Vertex rightChild : rightChildren) size += rightChild.getSize();
				for (Vertex leftChild : leftChildren) size += leftChild.getSize();
				return size;
			}

			void sort() {
				List<Vertex> temp = new ArrayList<>(leftChildren);
				temp.sort((o1, o2) -> Integer.compare(o2.getSize(), o1.getSize()));
				leftChildren.clear();
				int count = 0;
				int i = 0;
				while (count < temp.size()) {
					leftChildren.add(temp.get(i));
					count++;
					if (count < temp.size()) {
						leftChildren.add(temp.get(temp.size() - i - 1));
						count++;
					}
					i++;
				}
			}

		}

	}

}
