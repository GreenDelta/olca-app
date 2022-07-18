package org.openlca.app.editors.graphical.layouts;

import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.figures.NodeFigure;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.util.Labels;

import java.util.*;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;

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

	private final Graph graph;
	private final Node referenceNode;

	private Map<NodeFigure, Point> mapFigureToLocation = new HashMap<>();
	/** A map keeping track of nodes laid out by the TreeLayout. */
	private final Map<Node, Vertex> mapNodeToVertex = new HashMap<>();
	private IFigure parentFigure;


	public GraphLayout(GraphEditPart graphEditPart) {
		this.graph = graphEditPart.getModel();
		this.referenceNode = graph.getReferenceNode();
	}

	@Override
	public void layout(IFigure parent) {
		this.parentFigure = parent;
		var offset = getOrigin(parent);

		// Reset the mapFigureToLocation if a figure has been added/removed.
		var childrenSet = new HashSet<NodeFigure>();
		for (var child : parent.getChildren())
			if (child instanceof NodeFigure figure)
				childrenSet.add(figure);
		if (!childrenSet.equals(mapFigureToLocation.keySet())) {
			mapFigureToLocation.clear();
			mapNodeToVertex.clear();
		}

		for (var child : parent.getChildren()) {
			if (child instanceof NodeFigure figure) {
				var constraint = (Rectangle) getConstraint(figure);
				if (constraint == null)
					continue;

				var bounds = new Rectangle(
					calculateLocation(figure, constraint),
					calculateSize(figure, constraint));
				figure.setBounds(bounds.getTranslated(offset));
			}
		}
	}

	private Point calculateLocation(NodeFigure figure, Rectangle constraint) {
		// Set the location to the TreeLayout location if the figure has not been
		// moved manually.
		return constraint.getLocation().x == Integer.MAX_VALUE
			|| constraint.getLocation().y == Integer.MAX_VALUE
			? calculatePreferredLocation(figure)
			: constraint.getLocation().getCopy();
	}

	protected Dimension calculateSize(IFigure figure, Rectangle constraint) {
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
		if (mapFigureToLocation == null || mapFigureToLocation.isEmpty()) {
			layoutAsTree();
			layoutRestAsStack();
			if (mapFigureToLocation == null)
				mapFigureToLocation = new HashMap<>();
			mapFigureToLocation.clear();
			mapFigureToLocation = getLocationMap();
		}
		return mapFigureToLocation.get(figure);
	}

	private void layoutAsTree() {
		var inputLayout = new TreeLayout(INPUT, referenceNode);
		if (inputLayout.apexVertex == null)
			return;
		inputLayout.run();
		var outputLayout = new TreeLayout(OUTPUT, referenceNode);
		if (outputLayout.apexVertex == null)
			return;
		outputLayout.run();
	}

	/**
	 * Return the location of top left corner of the nodes.
	 */
	private Map<NodeFigure, Point> getLocationMap() {
		var nodeFigureToLocationMap = new HashMap<NodeFigure, Point>();
		for (var vertex : mapNodeToVertex.values()) {
			nodeFigureToLocationMap.put(vertex.figure, vertex.getLocation());
		}
		return nodeFigureToLocationMap;
	}

	private void layoutRestAsStack() {
		var stackFigures = getStackFigures();
		if (!stackFigures.isEmpty())
			new StackLayout(stackFigures, getReferenceNodeFigure()).run();
		}

		private NodeFigure getReferenceNodeFigure() {
			for (var child : parentFigure.getChildren()) {
				if (child instanceof NodeFigure figure) {
					if (figure.node == graph.getReferenceNode())
						return figure;
				}
			}
			return null;
		}

	private List<NodeFigure> getStackFigures() {
		var stackFigures = new ArrayList<NodeFigure>();
		for (var child : parentFigure.getChildren()) {
			if (child instanceof NodeFigure figure) {
				if (!mapNodeToVertex.containsKey(figure.node)
					|| mapNodeToVertex.get(figure.node).getLocation() == null)
					stackFigures.add(figure);
			}
		}
		stackFigures.sort(
			Comparator.comparing(figure -> Labels.name(figure.node.descriptor)));
		return stackFigures;
	}

	/**
	 * TreeLayout computes the location of the nodes of a vertex/edge tree.
	 * The tree can be oriented in two ways: left to right for the output side of
	 * the reference/root node and right to left for the input side.
	 * For the theory behind this algorithm, see:
	 * <ul>
	 * <li><a href="https://www.researchgate.net/publication/30508504_Improving_Walker%27s_Algorithm_to_Run_in_Linear_Time">Improving Walker's Algorithm to Run in Linear Time</a>,</li>
	 * <li><a href="http://www.cs.unc.edu/techreports/89-034.pdf">A Node-Positioning
	 * Algorithm for General Trees</a>,</li>
	 * <li><a href="https://github.com/prefuse/Prefuse/blob/master/src/prefuse/action/layout/graph/NodeLinkTreeLayout.java">Prefuse on Github</a>.</li>
	 * </ul>
	 *
	 * The product system being a graph, the tree algorithm is not enough to lay
	 * out every linked nodes of it. For that purpose, a node can have a
	 * mistletoe. A mistletoe is a branch of the tree that does not expand in the
	 * direction. For example, a provider of the reference node can also be the
	 * provider of a node that does not chain with the reference node.
	 * To better understand how the algorithm works, let's take a reference
	 * node (A), two providers (B and C) and a secondary recipient of B (D).
	 * To expand the node B, the providers B and C are shifted from a depth of 2
	 * to a depth of 3.
	 * <pre>
	 *
	 *                       2     1      3     2     1
	 *                       B ━┓         B ━━━ D ━┓
	 *                          ┣━ A  =>           ┣━ A
	 *                       C ━┛         C ━━━━━━━┛
	 *</pre>
	 * The node B has a mistletoe D. In order to determine the depths of the nodes
	 * of the main tree, the mistletoes' depths should be computed (first walks
	 * of the mistletoes). Then the position of each subtree of the main tree are
	 * determined by the first and second walk. Once, the positions of each node
	 * of the main tree are determined, the final positions of the apex are thus
	 * fixed such that the final positions of the mistletoes can be computed
	 * (second walks of the mistletoe).
	 */
	public class TreeLayout {

		private final int side;
		private final double DISTANCE_SIBLING = 16;
		private final double DISTANCE_SUBTREE = 32;
		private final double DISTANCE_LEVEL = 64;

		/**
		 * The reference node of the tree (to differentiate with the root of a
		 * subtree).
		 */
		private final Vertex apexVertex;

		/**
		 * Size of the levels (width of the columns in our case) of the tree.
		 * The index in this array is the depth of the vertex (the root is at depth
		 * 0).
		 */
		private final List<Double> levelSizes;
		private final List<Double> mistletoeSizes;
		/**
		 * The maximum depth of the tree (set to zero if there is only an apex
		 * node).
		 */
		private int maxDepth = 0;
		private List<Double> levels;

		TreeLayout(int side, Node apex) {
			this.side = side;
			apexVertex = createApexVertex(apex);
			createTree(apexVertex, 0);
			levelSizes = new ArrayList<>(Collections.nCopies(maxDepth + 2, 0.0));
			mistletoeSizes = new ArrayList<>(Collections.nCopies(maxDepth + 2, 0.0));
		}

		private Vertex createApexVertex(Node apex) {
			var vertex = new Vertex(apex, 0);
			if (vertex.figure == null
				|| getConstraint(vertex.figure) == null)  // see layout()
				return null;

			// Only the first Vertex of node is added to the map.
			mapNodeToVertex.putIfAbsent(apex, vertex);
			return vertex;
		}

		/**
		 * Calculate the center location of the vertex.
		 */
		private Point calculateStartLocation(Vertex vertex) {
			var previousVertex = mapNodeToVertex.get(vertex.node);
			if (previousVertex != null)
				return previousVertex.startLocation;

			var constraint = (Rectangle) getConstraint(vertex.figure);
			if (constraint == null) {
				return null;
			}

			// TODO Keep location from last layout
			// If the node has not been moved by the user. Its startLocation is null.
			return (constraint.getLocation().x == Integer.MAX_VALUE
				|| constraint.getLocation().y == Integer.MAX_VALUE)
				? null
				: constraint.getLocation().getTranslated(vertex.getSize().scale(0.5));
		}

		private Point calculateEndLocation(Vertex vertex) {
			var previousVertex = mapNodeToVertex.get(vertex.node);
			return (previousVertex == null || previousVertex.endLocation == null)
				? vertex.startLocation
				: previousVertex.endLocation;
		}

		public void run() {
			// Set the locations of the apex vertex.
			setApexLocation();

			// Do the first passes of the mistletoes to determine the total level
			// sizes.
			firstWalkMistletoes(apexVertex, 1);

			// Do the first pass  of the main tree (assign preliminary y-coordinate).
			firstWalk(apexVertex, 0, 1);

			// Sum-up level sizes.
			determineLevels();

			// Do the second pass (assign the main tree final positions)
			secondWalk(apexVertex, null, -apexVertex.prelim, 0);

			// Do the mistletoes second pass
			secondWalkMistletoes(apexVertex);
		}

		private void secondWalkMistletoes(Vertex vertex) {
			if (vertex != apexVertex && vertex.mistletoe != null) {
				var apex = vertex.mistletoe.apexVertex;
				vertex.mistletoe.setApexLocation();
				vertex.mistletoe.secondWalk(apex, null, -apex.prelim, 0);

				// Once, the position of the apex is determined, the second walks on the
				// mistletoes of the mistletoe are run.
				vertex.mistletoe.secondWalkMistletoes(apex);
			}

			for (var child : vertex.children) {
				secondWalkMistletoes(child);
			}
		}

		private void setApexLocation() {
			var calculatedStartLocation = calculateStartLocation(apexVertex);
			apexVertex.startLocation = calculatedStartLocation == null
				? new Point(0, 0).getTranslated(apexVertex.getSize().scale(0.5))
				: calculatedStartLocation;
			apexVertex.endLocation = calculateEndLocation(apexVertex);
		}

		/**
		 * Determine the size of each level of the tree.
		 * Return the size of the last level for convenience.
		 */
		private Double determineLevels() {
			levels = new ArrayList<>(Collections.nCopies(maxDepth + 1, 0.0));
			for (int i = 1; i <= maxDepth; ++i)
				levels.set(i, levels.get(i - 1)
					+ (levelSizes.get(i + 1) + levelSizes.get(i)) / 2
					+ DISTANCE_LEVEL
					+ mistletoeSizes.get(i + 1));
			return levels.get(levels.size() - 1);
		}

		private void firstWalkMistletoes(Vertex vertex, int depth) {
			if (vertex != apexVertex && vertex.mistletoe != null) {
				// First walks on the mistletoes of the mistletoe.
				vertex.mistletoe.firstWalkMistletoes(vertex.mistletoe.apexVertex, 1);

				// First walk on the mistletoe.
				vertex.mistletoe.firstWalk(vertex.mistletoe.apexVertex, 0, 1);
				mistletoeSizes.set(depth, Math.max(
					mistletoeSizes.get(depth), vertex.mistletoe.determineLevels()));
			}

			for (var child : vertex.children) {
				firstWalkMistletoes(child, depth + 1);
			}
		}

		/**
		 * Create the vertices of a tree which root is <code>parent</code>>.
		 * It works recursively by calling this method on children that have at
		 * least one child.
		 * @param parent The apex of the (sub-)tree to be created.
		 *
		 */
		private void createTree(Vertex parent, int depth) {
			var links = side == INPUT
				? parent.node.getAllTargetConnections()
				: parent.node.getAllSourceConnections();
			var children = new ArrayList<Node>();

			// Create the list of children of parent.
			for (var link : links) {
				var child = side == INPUT
					? link.getSourceNode()
					: link.getTargetNode();
				// Check if this child has not been already added by a neighbor, an
				// ancestor or the root of the subtree itself.
				if (!mapNodeToVertex.containsKey(child) && !children.contains(child))
					children.add(child);
			}

			if (!children.isEmpty())
				maxDepth = Math.max(maxDepth, depth + 1);

			children.sort(Comparator.comparing(node -> Labels.name(node.descriptor)));

			// Create the vertices of the children.
			for (var child : children) {
				var index = children.indexOf(child);
				var childVertex = new Vertex(child, index);
				childVertex.setParent(parent);
				if (childVertex.figure == null
					|| getConstraint(childVertex.figure) == null)  // see layout()
					continue;
				childVertex.setStartLocation(calculateStartLocation(childVertex));
				if (children.indexOf(child) != 0)
					childVertex.setPreviousSibling(
						mapNodeToVertex.get(children.get(index - 1)));

				// Check if this vertex is a mistletoe.
				if (isMistletoe(childVertex)) {
					var otherSide = side == INPUT ? OUTPUT : INPUT;
					childVertex.mistletoe = new TreeLayout(otherSide, child);
				}

				mapNodeToVertex.put(child, childVertex);
				parent.addChild(childVertex);
				createTree(childVertex, depth + 1);
			}
		}

		private void firstWalk(Vertex vertex, int number, int depth) {
			vertex.number = number;
			updateLevelSizes(depth, vertex);

			if (vertex.getChildCount() == 0) {  // is a leaf.
				vertex.prelim = vertex.previousSibling == null  // is the first child.
					? 0
					: vertex.previousSibling.prelim
					+ spacingOf(vertex.previousSibling, vertex, true);
			}
			else {
				var firstChild = vertex.getFirstChild();
				var lastChild = vertex.getLastChild();
				if (firstChild != null && lastChild != null) {
					var defaultAncestor = firstChild;
					var child = firstChild;
					for (int i = 0; child != null; ++i, child = child.getNextSibling()) {
						firstWalk(child, i, depth + 1);
						defaultAncestor = apportion(child, defaultAncestor);
					}

					executeShifts(vertex);

					var midpoint = (firstChild.prelim + lastChild.prelim) / 2;

					if (vertex.previousSibling != null) {  // is not the first child.
						vertex.prelim = vertex.previousSibling.prelim
							+ spacingOf(vertex.previousSibling, vertex, true);
						vertex.modifier = vertex.prelim - midpoint;
					} else {
						vertex.prelim = midpoint;
					}
				}
			}
		}

		private void executeShifts(Vertex vertex) {
			double shift = 0, change = 0;
			for (var child = vertex.getLastChild(); child != null;
					 child = child.previousSibling ) {
				child.prelim += shift;
				child.modifier += shift;
				change += child.change;
				shift += child.shift + change;
			}
		}

		/**
		 * This procedure cleans up the positioning of small sibling subtrees.
		 * When moving a new subtree farther and farther down, gaps may open up
		 * among smaller subtrees that were previously sandwiched between larger
		 * subtrees. Thus, when moving the new, larger subtree down, the distance it
		 * is moved is also apportioned to smaller, interior subtrees.
		 */
		private Vertex apportion(Vertex vertex, Vertex ancestor) {
			var previousSibling = vertex.previousSibling;
			if (previousSibling != null) {
				var downInsideVertex = vertex;
				var downOutsideVertex = vertex;
				var topInsideVertex = previousSibling;
				var topOutsideVertex = downInsideVertex.parent.getFirstChild();

				var downInsideShift = downInsideVertex.modifier;
				var downOutsideShift = downOutsideVertex.modifier;
				var topInsideShift = topInsideVertex.modifier;
				var topOutsideShift = topOutsideVertex.modifier;

				var nextBottom = nextBottom(topInsideVertex);
				var nextTop = nextTop(downInsideVertex);
				while ( nextBottom != null && nextTop != null ) {
					topInsideVertex = nextBottom;
					downInsideVertex = nextTop;
					topOutsideVertex = nextTop(topOutsideVertex);
					downOutsideVertex = nextBottom(downOutsideVertex);
					downOutsideVertex.ancestor = vertex;
					var shift = (topInsideVertex.prelim + topInsideShift)
						- (downInsideVertex.prelim + downInsideShift)
						+ spacingOf(topInsideVertex, downInsideVertex, false);
					if (shift > 0) {
						moveSubtree(ancestor(topInsideVertex, vertex, ancestor), vertex, shift);
						downInsideShift += shift;
						downOutsideShift += shift;
					}
					topInsideShift += topInsideVertex.modifier;
					downInsideShift += downInsideVertex.modifier;
					topOutsideShift += topOutsideVertex.modifier;
					downOutsideShift += downOutsideVertex.modifier;

					nextBottom = nextBottom(topInsideVertex);
					nextTop = nextTop(downInsideVertex);
				}
				if (nextBottom != null && nextBottom(downOutsideVertex) == null) {
					downOutsideVertex.thread = nextBottom;
					downOutsideVertex.modifier += topInsideShift - downOutsideShift;
				}
				if (nextTop != null && nextTop(topOutsideVertex) == null ) {
					topOutsideVertex.thread = nextTop;
					topOutsideVertex.modifier += downInsideShift - topOutsideShift;
					ancestor = vertex;
				}
			}
			return ancestor;
		}

		private void secondWalk(Vertex vertex, Vertex parent, double modifierSum,
														int depth) {
			var x = (int) (apexVertex.endLocation.x
				+ Math.round(levels.get(depth)) * (side == OUTPUT ? 1 : -1));
			var y = (int) (apexVertex.endLocation.y + vertex.prelim + modifierSum);
			var location = new Point(x, y);
			vertex.setLocation(location, parent);

			for (var child : vertex.children)
				secondWalk(child, vertex, modifierSum + vertex.modifier, depth + 1);
		}

		private Vertex nextTop(Vertex vertex) {
			var child = vertex.getFirstChild();
			return child != null ? child : vertex.thread;
		}

		private Vertex nextBottom(Vertex vertex) {
			var child = vertex.getLastChild();
			return child != null ? child : vertex.thread;
		}

		private void moveSubtree(Vertex wm, Vertex wp, double shift) {
			double subtrees = wp.number - wm.number;
			wp.change -= shift/subtrees;
			wp.shift += shift;
			wm.change += shift/subtrees;
			wp.prelim += shift;
			wp.modifier += shift;
		}

		private double spacingOf(Vertex left, Vertex right, boolean areSiblings) {
			var neighborSeparation = areSiblings ? DISTANCE_SIBLING : DISTANCE_SUBTREE;
			var heightsMean = (left.getSize().height + right.getSize().height) / 2;
			return neighborSeparation + heightsMean;
		}

		private Vertex ancestor(Vertex topInsideVertex, Vertex vertex, Vertex ancestor) {
			return (topInsideVertex.ancestor != null
				&& topInsideVertex.ancestor.parent == vertex.parent)
				? topInsideVertex.ancestor
				: ancestor;
		}

		private boolean isMistletoe(Vertex vertex) {
			var links = side == INPUT
				? vertex.node.getAllSourceConnections()
				: vertex.node.getAllTargetConnections();
			for (var link : links) {
				if (link.isCloseLoop())
					continue;

				var otherNode = (side == INPUT)
					? link.getTargetNode()
					: link.getSourceNode();
				if (!mapNodeToVertex.containsKey(otherNode)) {
					return true;
				}
			}
			return false;
		}

		private void updateLevelSizes(int depth, Vertex vertex) {
			double d = vertex.getSize().width();
			levelSizes.set(depth, Math.max(levelSizes.get(depth), d));
		}

	}

	public class StackLayout {

		private final double DISTANCE_LEVEL = 48;

		private final List<NodeFigure> figures = new ArrayList<>();
		private final NodeFigure rootFigure;

		StackLayout(List<NodeFigure> stackFigures, NodeFigure rootFigure) {
			this.rootFigure = rootFigure;
			figures.add(rootFigure);
			figures.addAll(stackFigures);
		}

		public void run() {

			var maxDepth = figures.size();

			var levels = new ArrayList<>(Collections.nCopies(maxDepth, 0.0));
			var rootLocation = mapNodeToVertex.get(rootFigure.node).endLocation;

			for (int i = 1; i < maxDepth; ++i) {
				levels.set(i, levels.get(i - 1)
					+ (heightOfFigure(i - 1) + heightOfFigure(i)) / 2
					+ DISTANCE_LEVEL);
				var vertex = new Vertex(figures.get(i).node, i);
				var x = rootLocation.x;
				var y = (int) Math.round(rootLocation.y + levels.get(i));
				vertex.setLocation(new Point(x, y),
					mapNodeToVertex.get(graph.getReferenceNode()));
				mapNodeToVertex.put(figures.get(i).node, vertex);
			}
		}

		private int heightOfFigure(int index) {
			return calculateSize(figures.get(index),
				(Rectangle) getConstraint(figures.get(index))).height();
		}

	}

	private class Vertex {

		private final Node node;
		private final NodeFigure figure;

		private Vertex parent;
		private final List<Vertex> children = new ArrayList<>();
		public int siblingIndex;
		private Vertex previousSibling;

		public double prelim;
		public double modifier;

		public Vertex thread;
		public Vertex ancestor;
		public double number;
		public double change;
		public double shift;

		/** A mistletoe is a vertex that has more than one parent. This abnormal
		 * tree is laid out after the main tree.
		 */
		public TreeLayout mistletoe;

		public Point startLocation;
		public Point endLocation;

		public Vertex(Node node, int siblingIndex) {
			this.siblingIndex = siblingIndex;
			this.node = node;
			this.figure = getFigure(node);
		}

		private NodeFigure getFigure(Node node) {
			var viewer = (GraphicalViewer) graph.editor.getAdapter(
				GraphicalViewer.class);
			var registry = viewer.getEditPartRegistry();
			if (registry.get(node) instanceof NodeEditPart nodeEditPart)
				return nodeEditPart.getFigure();
			else
				return null;
		}

		public Dimension getSize() {
			return calculateSize(figure, (Rectangle) getConstraint(figure));
		}

		public void addChild(Vertex childVertex) {
			children.add(childVertex);
		}

		public int getChildCount() {
			return children.size();
		}

		public void setParent(Vertex parent) {
			this.parent = parent;
		}

		public void setPreviousSibling(Vertex vertex) {
			previousSibling = vertex;
		}

		public Vertex getNextSibling() {
			if (parent == null) return null;
			return (parent.getLastChild() == this)
				? null
				: parent.children.get(siblingIndex + 1);
		}

		public Point getStartLocation() {
			return startLocation;
		}

		public Point getEndLocation() {
			return endLocation;
		}

		public Point getLocation() {
			if (endLocation == null)
				return null;
			else return endLocation.getTranslated(getSize().getScaled(0.5).getNegated());
		}

		public Vertex getFirstChild() {
			return (children.isEmpty()) ? null : children.get(0);
		}

		public Vertex getLastChild() {
			return (children.isEmpty()) ? null : children.get(children.size() - 1);
		}

		public void setLocation(Point location, Vertex parent) {
			endLocation = location;
			if (startLocation == null)
				startLocation = parent != null
					? parent.getStartLocation()
					: location;
		}

		public void setStartLocation(Point location) {
			startLocation = location;
		}

		public void clear() {
			children.clear();
			siblingIndex = 0;
			previousSibling = null;
			prelim = 0.0;
			modifier = 0.0;
			thread = null;
			ancestor = null;
			number = 0.0;
			change = 0.0;
			shift	= 0.0;
		}

		public String toString() {
			return "Vertex of: " + node + " at " + startLocation;
		}

	}

}
