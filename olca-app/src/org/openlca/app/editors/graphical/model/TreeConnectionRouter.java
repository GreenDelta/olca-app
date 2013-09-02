package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.PointList;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;

public class TreeConnectionRouter extends BendpointConnectionRouter {

	private static final TreeConnectionRouter instance = new TreeConnectionRouter();

	public static TreeConnectionRouter get() {
		return instance;
	}

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
		if (conn.getSourceAnchor().getOwner() != null
				&& conn.getTargetAnchor().getOwner() != null) {
			PointList points = new PointList();
			points.addPoint(conn.getPoints().getFirstPoint());

			ProcessFigure source = getProcessFigure(conn.getSourceAnchor());
			ProcessFigure target = getProcessFigure(conn.getTargetAnchor());

			if (target.getLocation().x < source.getLocation().x
					+ source.getSize().width
					|| target.getLocation().x > source.getLocation().x
							+ source.getSize().width
							+ GraphLayoutManager.HORIZONTAL_SPACING
							+ target.getSize().width || target == source) {
				points.addPoint(conn
						.getPoints()
						.getFirstPoint()
						.getTranslated(
								GraphLayoutManager.HORIZONTAL_SPACING / 2, 0));

				int y1 = source.getLocation().y < target.getLocation().y ? target
						.getLocation().y
						- GraphLayoutManager.VERTICAL_SPACING
						/ 2 : source.getLocation().y
						- GraphLayoutManager.VERTICAL_SPACING / 2;

				points.addPoint(
						conn.getPoints()
								.getFirstPoint()
								.getTranslated(
										+GraphLayoutManager.HORIZONTAL_SPACING / 2,
										0).x, y1);

				points.addPoint(
						conn.getPoints()
								.getLastPoint()
								.getTranslated(
										-GraphLayoutManager.HORIZONTAL_SPACING / 2,
										0).x, y1);

				points.addPoint(conn
						.getPoints()
						.getLastPoint()
						.getTranslated(
								-GraphLayoutManager.HORIZONTAL_SPACING / 2, 0));
			} else {
				points.addPoint(conn
						.getPoints()
						.getFirstPoint()
						.getTranslated(
								GraphLayoutManager.HORIZONTAL_SPACING / 2, 0));

				points.addPoint(
						conn.getPoints()
								.getFirstPoint()
								.getTranslated(
										GraphLayoutManager.HORIZONTAL_SPACING / 2,
										0).x, conn.getPoints().getLastPoint().y);
			}

			points.addPoint(conn.getPoints().getLastPoint());
			conn.setPoints(points);
		}
	}

}
