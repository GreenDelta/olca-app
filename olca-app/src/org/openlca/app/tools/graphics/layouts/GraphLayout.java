package org.openlca.app.tools.graphics.layouts;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.tools.graphics.BasicGraphicalEditor;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.model.Component;

import java.util.*;

import static org.eclipse.draw2d.PositionConstants.*;

public abstract class GraphLayout extends FreeformLayout implements
		LayoutInterface {

	/** Integer.MAX_VALUE is used as a default value with the limited risk of
	 * having a figure in that area of the canvas.
	 */
	public static final Point DEFAULT_LOCATION =
			new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);

	private final int orientation;
	private final BasicGraphicalEditor editor;
	public double distanceSibling = 16;
	public double distanceSubtree = 32;
	public double distanceLevel = 64;

	private Map<Figure, Point> mapFigureToLocation = new HashMap<>();
	/** A map keeping track of nodes laid out by the TreeLayout. */
	final Map<Component, Vertex> mapNodeToVertex = new HashMap<>();
	private IFigure parentFigure;
	/**
	 * Figures are usually laid out in two times as children are laid out first.
	 * This flag allows to execute tasks after the pre-layout.
	 */
	private boolean preLayout = false;

	public GraphLayout(BasicGraphicalEditor editor, int orientation) {
		this.editor = editor;
		this.orientation = orientation;
	}

	public BasicGraphicalEditor getEditor() {
		return editor;
	}

	@Override
	public void layout(IFigure parent) {
		this.parentFigure = parent;
		var offset = getOrigin(parent);

		// Reset the mapFigureToLocation if a figure has been added/removed.
		var childrenSet = new HashSet<ComponentFigure>();
		for (var child : parent.getChildren())
			if (child instanceof ComponentFigure figure)
				childrenSet.add(figure);
		if (!childrenSet.equals(mapFigureToLocation.keySet())) {
			mapFigureToLocation.clear();
			mapNodeToVertex.clear();
		}

		for (var child : parent.getChildren()) {
			if (child instanceof Figure figure) {
				var constraint = (Rectangle) getConstraint(figure);
				if (constraint == null)
					continue;

				var bounds = new Rectangle(
						calculateLocation(figure, constraint),
						calculateSize(figure, constraint));
				figure.setBounds(bounds.getTranslated(offset));
			}
		}
		if (preLayout) focusOnStart();
		if (!preLayout) preLayout = true;
	}

	protected void focusOnStart() {
		if (!editor.wasFocused) {
			editor.wasFocused = editor.doFocus();
		}
	}

	private Point calculateLocation(Figure figure, Rectangle constraint) {
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

	protected Dimension getConstrainedSize(IFigure figure) {
		return calculateSize(figure, (Rectangle) getConstraint(figure));
	}

	private Point calculatePreferredLocation(Figure figure) {
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
		var inputLayout =
				new TreeLayout(this, orientation, getReferenceNode(), true);
		if (inputLayout.apexVertex == null)
			return;
		inputLayout.run();
		var outputLayout =
				new TreeLayout(this, orientation, getReferenceNode(), false);
		if (outputLayout.apexVertex == null)
			return;
		outputLayout.run();
	}

	/**
	 * Return the location of top left corner of the nodes.
	 */
	private Map<Figure, Point> getLocationMap() {
		var nodeFigureToLocationMap = new HashMap<Figure, Point>();
		for (var vertex : mapNodeToVertex.values()) {
			nodeFigureToLocationMap.put(vertex.figure, vertex.getLocation());
		}
		return nodeFigureToLocationMap;
	}

	private void layoutRestAsStack() {
		var stackFigures = getStackFigures();
		if (!stackFigures.isEmpty()) {
			var dir = (orientation & (EAST | WEST)) != 0 ? SOUTH : EAST;
			new StackLayout(this, stackFigures, getReferenceFigure(), dir).run();
		}
	}

	private List<ComponentFigure> getStackFigures() {
		var stackFigures = new ArrayList<ComponentFigure>();
		for (var child : parentFigure.getChildren()) {
			if (child instanceof ComponentFigure figure) {
				if (!mapNodeToVertex.containsKey(figure.getComponent())
						|| mapNodeToVertex.get(figure.getComponent()).getLocation() == null)
					stackFigures.add(figure);
			}
		}
		stackFigures.sort(
				Comparator.comparing(
						figure -> figure.getComponent().getComparisonLabel()));
		return stackFigures;
	}

	protected IFigure getParentFigure() {
		return parentFigure;
	}

	public void setDistanceSibling(int distance) {
		distanceSibling = distance;
	}

	public void setDistanceSubtree(int distance) {
		distanceSubtree= distance;
	}

	public void setDistanceLevel(int distance) {
		distanceLevel = distance;
	}

}
