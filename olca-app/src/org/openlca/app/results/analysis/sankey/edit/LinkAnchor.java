package org.openlca.app.results.analysis.sankey.edit;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.results.analysis.sankey.figures.SankeyNodeFigure;

import static org.eclipse.draw2d.PositionConstants.*;

public class LinkAnchor extends AbstractConnectionAnchor {

	private final int anchor;
	private final boolean forInput;

	public LinkAnchor(IFigure figure, boolean forInput, int anchor) {
		super(figure);
		this.forInput = forInput;
		this.anchor = anchor;
	}

	@Override
	public Point getLocation(Point ref) {
		var owner = getOwner();

		if (owner instanceof SankeyNodeFigure figure) {
			var orientation = figure.node.getDiagram().orientation;

			var bounds = owner.getBounds();
			var point = initPointOf(bounds, orientation, forInput);

			var translator = ((orientation & (EAST | WEST)) != 0)
					? new Point(0, anchor != 0 ? anchor : bounds.height() / 2)
					: new Point(anchor != 0 ? anchor : bounds.width() / 2, 0);

			point.translate(translator);

			owner.translateToAbsolute(point);
			return point;
		}
		else return ref;
	}

	private static Point initPointOf(Rectangle bounds, int orientation,
	 boolean forInput) {
		if (forInput)
			// EAST and SOUTH init point is the same.
			return switch (orientation) {
				case WEST -> bounds.getTopRight();
				case NORTH -> bounds.getBottomLeft();
				default -> bounds.getTopLeft();
			};
		else return switch (orientation) {
			case EAST -> bounds.getTopRight();
			case SOUTH -> bounds.getBottomLeft();
			default -> bounds.getTopLeft();
		};
	}

}
