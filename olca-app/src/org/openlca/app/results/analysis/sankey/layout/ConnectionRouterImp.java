package org.openlca.app.results.analysis.sankey.layout;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.openlca.app.results.analysis.sankey.model.ProcessFigure;

public class ConnectionRouterImp extends BendpointConnectionRouter {

	@Override
	public void route(Connection conn) {
		NULL.route(conn);
		PointList points = conn.getPoints();
		Point start = points.getFirstPoint();
		Point end = points.getLastPoint();
		points.removeAllPoints();
		points.addPoint(start);
		if (start.y > end.y)
			routeBottomToTop(start, end, points);
		else
			routeTopToBottom(start, end, points);
		points.addPoint(end);
	}

	private void routeBottomToTop(Point start, Point end, PointList points) {
		int midY = end.y + (start.y - end.y) / 2;
		points.addPoint(start.x, midY);
		points.addPoint(end.x, midY);
	}

	private void routeTopToBottom(Point start, Point end, PointList points) {
		int offsetY = GraphLayoutManager.verticalSpacing / 4;
		int midX = 0;
		if (Math.abs(start.x - end.x) < ProcessFigure.WIDTH) {
			midX = Math.max(start.x, end.x) + ProcessFigure.WIDTH;
		} else {
			midX = start.x + (end.x - start.x) / 2;
		}
		points.addPoint(start.x, start.y - offsetY);
		points.addPoint(midX, start.y - offsetY);
		points.addPoint(midX, end.y + offsetY);
		points.addPoint(end.x, end.y + offsetY);
	}
}
