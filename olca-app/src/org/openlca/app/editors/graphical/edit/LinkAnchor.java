package org.openlca.app.editors.graphical.edit;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graphical.figures.ExchangeFigure;
import org.openlca.app.editors.graphical.figures.NodeFigure;

public class LinkAnchor extends AbstractConnectionAnchor {

	private final boolean forInput;

	public LinkAnchor(IFigure figure, boolean forInput) {
		super(figure);
		this.forInput = forInput;
	}

	@Override
	public Point getLocation(Point ref) {
		var owner = getOwner();
		if (owner == null)
			return ref;

		// for nodes, it is just the middle
		// of the left or right side
		var bounds = owner.getBounds();
		var point = forInput
			? bounds.getLeft()
			: bounds.getRight();
		owner.translateToAbsolute(point);

		if (owner instanceof NodeFigure)
			return point;

		// for exchanges, we move the anchor to the
		// left or right side of the surrounding
		// process box
		else if (owner instanceof ExchangeFigure exchangeFigure) {
			var nodeFigure = getNodeOwner();

			if (nodeFigure == null)
				return point;

			// set x to the right or left side of
			// the surrounding box

			var outerBounds = nodeFigure.getBounds();
			var outer = forInput
				? outerBounds.getLeft()
				: outerBounds.getRight();
			nodeFigure.translateToAbsolute(outer);
			point.x = outer.x;
			return point;
		}
		else return point;
	}

	public NodeFigure getNodeOwner() {
		if (getOwner() instanceof NodeFigure nodeFigure)
			return nodeFigure;
		else if (getOwner() instanceof ExchangeFigure exchangeFigure)
			return exchangeFigure.getNodeFigure();
		else return null;
	}

}
