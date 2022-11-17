package org.openlca.app.tools.graphics.figures;

import org.eclipse.draw2d.ArrowLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;

import java.util.Objects;

import static org.openlca.app.tools.graphics.figures.Connection.ROUTER_CURVE;

public class TangentialArrowLocator extends ArrowLocator {

	public TangentialArrowLocator(org.eclipse.draw2d.Connection connection, int location) {
		super(connection, location);
	}

	@Override
	public void relocate(IFigure target) {
		var points = getConnection().getPoints();
		var arrow = (PolygonDecoration) target;
		arrow.setLocation(getLocation(points));

		if (getConnection() instanceof Connection con
				&& Objects.equals(con.getType(), ROUTER_CURVE)) {
				// decorationSize is roughly the size of the polygon decoration.
				var decorationSize = Math.max(
						arrow.getPoints().getBounds().getSize().width(),
						arrow.getPoints().getBounds().getSize().height());
				var pathIterator = con.getPathIterator();

				while (!pathIterator.isDone()) {
					var point = Connection.nextPoint(pathIterator);
					if (getAlignment() == SOURCE) {
						var dist = points.getPoint(0).getDistance(point);
						if (dist > decorationSize)
							arrow.setReferencePoint(point);
					}
					else if (getAlignment() == TARGET) {
						var dist = points.getPoint(points.size() - 1).getDistance(point);
						if (dist > decorationSize)
							arrow.setReferencePoint(point);
					}
					pathIterator.next();
				}
				// If the connection is too short for the reference point to be on the
				// line, the default values are returned.
			}
		else if (getAlignment() == SOURCE)
			arrow.setReferencePoint(points.getPoint(1));
		else if (getAlignment() == TARGET)
			arrow.setReferencePoint(points.getPoint(points.size() - 2));
	}

}
