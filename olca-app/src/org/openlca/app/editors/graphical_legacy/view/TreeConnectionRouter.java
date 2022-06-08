package org.openlca.app.editors.graphical_legacy.view;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.PointList;
import org.openlca.app.editors.graphical_legacy.layout.LayoutManager;

public class TreeConnectionRouter extends BendpointConnectionRouter {

	public static final TreeConnectionRouter instance = new TreeConnectionRouter();

	private TreeConnectionRouter() {
	}

	@Override
	public void route(Connection conn) {
		super.route(conn);

		var source = processOf(conn.getSourceAnchor());
		var target = processOf(conn.getTargetAnchor());
		if (source == null || target == null)
			return;
		var points = new PointList();
		points.addPoint(conn.getPoints().getFirstPoint());

		var sourceLoc = source.getLocation();
		var targetLoc = target.getLocation();
		var firstPoint = conn.getPoints().getFirstPoint();
		var lastPoint = conn.getPoints().getLastPoint();

		if (targetLoc.x < sourceLoc.x + source.getSize().width
				|| targetLoc.x > sourceLoc.x + source.getSize().width + LayoutManager.H_SPACE + target.getSize().width
				|| target == source) {
			points.addPoint(firstPoint.getTranslated(LayoutManager.H_SPACE / 2, 0));
			int y1 = Math.max(sourceLoc.y, targetLoc.y);
			y1 -= LayoutManager.V_SPACE / 2;
			points.addPoint(firstPoint.getTranslated(LayoutManager.H_SPACE / 2, 0).x, y1);
			points.addPoint(lastPoint.getTranslated(-LayoutManager.H_SPACE / 2, 0).x, y1);
			points.addPoint(lastPoint.getTranslated(-LayoutManager.H_SPACE / 2, 0));
		} else {
			points.addPoint(firstPoint.getTranslated(LayoutManager.H_SPACE / 2, 0));
			points.addPoint(firstPoint.getTranslated(LayoutManager.H_SPACE / 2, 0).x, lastPoint.y);
		}
		points.addPoint(lastPoint);
		conn.setPoints(points);
	}

	private ProcessFigure processOf(ConnectionAnchor anchor) {
		if (anchor == null)
			return null;
		var figure = anchor.getOwner();
		int depth = 0;  // just in case there are cycles
		while (figure != null && depth < 100) {
			if (figure instanceof ProcessFigure)
				return (ProcessFigure) figure;
			figure = figure.getParent();
			depth++;
		}
		return null;
	}
}
