package org.openlca.app.components.graphics.figures;

import static org.openlca.app.components.graphics.figures.Connection.*;

import java.util.Objects;

import org.eclipse.draw2d.ArrowLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;

public class TangentialArrowLocator extends ArrowLocator {

	/// Roughly the size of the default `PolygonDecoration`. This must be a
	/// constant: when it is calculated from the bounds of the rotated arrow, the
	/// arrow can flip between two rotations with slightly different bounds on
	/// each relocation, which changes the extent of the connection layer and
	/// results in an endless validation loop.
	private static final int DECORATION_SIZE = 10;

	public TangentialArrowLocator(
		org.eclipse.draw2d.Connection connection, int location
	) {
		super(connection, location);
	}

	@Override
	public void relocate(IFigure target) {
		var points = getConnection().getPoints();
		var arrow = (PolygonDecoration) target;
		arrow.setLocation(getLocation(points));

		if (getConnection() instanceof Connection con
			&& Objects.equals(con.getType(), ROUTER_CURVE)) {
			var it = con.getPathIterator();

			while (!it.isDone()) {
				var point = Connection.nextPoint(it);
				if (getAlignment() == SOURCE) {
					var dist = points.getPoint(0).getDistance(point);
					if (dist > DECORATION_SIZE) {
						arrow.setReferencePoint(point);
					}
				} else if (getAlignment() == TARGET) {
					var dist = points.getPoint(points.size() - 1).getDistance(point);
					if (dist > DECORATION_SIZE) {
						arrow.setReferencePoint(point);
					}
				}
				it.next();
			}
			// If the connection is too short for the reference point to be on the
			// line, the default values are returned.
		} else if (getAlignment() == SOURCE) {
			arrow.setReferencePoint(points.getPoint(1));
		} else if (getAlignment() == TARGET) {
			arrow.setReferencePoint(points.getPoint(points.size() - 2));
		}
	}

}
