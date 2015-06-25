package org.openlca.app.results.analysis.sankey.layout;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;

public class TreeConnectionRouter extends BendpointConnectionRouter {

	@Override
	public void route(Connection conn) {
		// pre route
		NULL.route(conn);

		// get points
		PointList points = conn.getPoints();
		Point first = points.getFirstPoint();
		Point last = points.getLastPoint();

		// distance from to point to connection anchor
		final int trans = GraphLayoutManager.verticalSpacing / 4;

		// create new list
		PointList newPoints = new PointList();
		// add first point
		newPoints.addPoint(first);

		// add 2 new points
		newPoints.addPoint(first.x, first.y - trans);
		newPoints.addPoint(last.x, last.y + trans);

		// add last point
		newPoints.addPoint(last);
		// set new list
		conn.setPoints(newPoints);
	}
}
