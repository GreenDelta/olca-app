package org.openlca.app.editors.graph.edit;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.editors.graph.figures.ExchangeFigure;
import org.openlca.app.editors.graph.figures.NodeFigure;

public class LinkAnchor extends AbstractConnectionAnchor {

	private final boolean forInput;

	public LinkAnchor(IFigure figure, boolean forInput) {
		super(figure);
		this.forInput = forInput;
	}

	@Override
	public Point getLocation(Point ref) {
		var owner = getOwner();
		System.out.printf("In getLocation, owner: %s\n", owner);
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
		else if (owner instanceof ExchangeFigure) {
			var nodeFigure = owner.getParent().getParent();

			if (nodeFigure == null)
				return point;

			// set x to the right or left side of
			// the surrounding box

			var outerBounds = nodeFigure.getBounds();
			var outer = forInput
				? outerBounds.getLeft()
				: outerBounds.getRight();
			nodeFigure.translateToAbsolute(outer);
			System.out.printf("    oldpoint: %s\n", point);
			point.x = outer.x;
			System.out.printf("    newpoint: %s\n", point);
			return point;
		}
		else return point;
	}

}
