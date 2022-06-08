package org.openlca.app.editors.graphical.layouts;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.PointList;
import org.openlca.app.editors.graphical.edit.LinkAnchor;

public class TreeConnectionRouter extends BendpointConnectionRouter {

	public static int H_SPACE = 25;
	public static int V_SPACE = 25;

	@Override
	public void route(Connection conn) {
		super.route(conn);

		var source = (conn.getSourceAnchor() instanceof LinkAnchor)
			? ((LinkAnchor) conn.getSourceAnchor()).getNodeOwner()
			: null;
		var target = (conn.getTargetAnchor() instanceof LinkAnchor)
			? ((LinkAnchor) conn.getTargetAnchor()).getNodeOwner()
			: null;

		if (source == null || target == null)
			return;
		var points = new PointList();
		points.addPoint(conn.getPoints().getFirstPoint());

		var sourceLoc = source.getLocation();
		var targetLoc = target.getLocation();
		var firstPoint = conn.getPoints().getFirstPoint();
		var lastPoint = conn.getPoints().getLastPoint();

		if (targetLoc.x < sourceLoc.x + source.getSize().width
				|| targetLoc.x > sourceLoc.x + source.getSize().width + H_SPACE + target.getSize().width
				|| target == source) {
			points.addPoint(firstPoint.getTranslated(H_SPACE / 2, 0));
			int y1 = Math.max(sourceLoc.y, targetLoc.y);
			y1 -= V_SPACE / 2;
			points.addPoint(firstPoint.getTranslated(H_SPACE / 2, 0).x, y1);
			points.addPoint(lastPoint.getTranslated(-H_SPACE / 2, 0).x, y1);
			points.addPoint(lastPoint.getTranslated(-H_SPACE / 2, 0));
		} else {
			points.addPoint(firstPoint.getTranslated(H_SPACE / 2, 0));
			points.addPoint(firstPoint.getTranslated(H_SPACE / 2, 0).x, lastPoint.y);
		}
		points.addPoint(lastPoint);
		conn.setPoints(points);
	}

}
