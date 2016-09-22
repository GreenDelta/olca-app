package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.openlca.app.editors.graphical.layout.LayoutManager;

public class TreeConnectionRouter extends BendpointConnectionRouter {

	public static final TreeConnectionRouter instance = new TreeConnectionRouter();

	private TreeConnectionRouter() {

	}

	private ProcessFigure getProcessFigure(ConnectionAnchor anchor) {
		if (anchor.getOwner() instanceof ProcessFigure)
			return (ProcessFigure) anchor.getOwner();
		else
			return (ProcessFigure) anchor.getOwner().getParent().getParent();
	}

	@Override
	public void route(Connection conn) {
		super.route(conn);
		if (conn.getSourceAnchor().getOwner() == null || conn.getTargetAnchor().getOwner() == null)
			return;
		PointList points = new PointList();
		points.addPoint(conn.getPoints().getFirstPoint());
		ProcessFigure source = getProcessFigure(conn.getSourceAnchor());
		ProcessFigure target = getProcessFigure(conn.getTargetAnchor());
		Point sourceLoc = source.getLocation();
		Point targetLoc = target.getLocation();
		Point firstPoint = conn.getPoints().getFirstPoint();
		Point lastPoint = conn.getPoints().getLastPoint();
		if (targetLoc.x < sourceLoc.x + source.getSize().width
				|| targetLoc.x > sourceLoc.x + source.getSize().width + LayoutManager.H_SPACE + target.getSize().width
				|| target == source) {
			points.addPoint(firstPoint.getTranslated(LayoutManager.H_SPACE / 2, 0));
			int y1 = sourceLoc.y < targetLoc.y ? targetLoc.y : sourceLoc.y;
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
}
