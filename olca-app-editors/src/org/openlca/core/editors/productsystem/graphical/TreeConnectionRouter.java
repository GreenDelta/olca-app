package org.openlca.core.editors.productsystem.graphical;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.PointList;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeFigure;
import org.openlca.core.editors.productsystem.graphical.model.ProcessFigure;

/**
 * Routes the connections of a tree (behaviour is similar to Manhattan
 * connection router)
 * 
 * @author Sebastian Greve
 * 
 */
public class TreeConnectionRouter extends BendpointConnectionRouter {

	/**
	 * Getter of the process figure of the anchor
	 * 
	 * @param anchor
	 *            The anchor the process figure is requested for
	 * @return The process figure of the anchor
	 */
	private ProcessFigure getProcessFigure(final ConnectionAnchor anchor) {
		ProcessFigure processFigure = null;
		if (anchor.getOwner() instanceof ProcessFigure) {
			processFigure = ((ProcessFigure) anchor.getOwner())
					.getProcessNode().getFigure();
		} else {
			processFigure = ((ExchangeFigure) anchor.getOwner())
					.getParentProcessFigure().getProcessNode().getFigure();
		}
		return processFigure;
	}

	@Override
	public void route(final Connection conn) {
		super.route(conn);
		if (conn.getSourceAnchor().getOwner() != null
				&& conn.getTargetAnchor().getOwner() != null) {
			final PointList points = new PointList();
			points.addPoint(conn.getPoints().getFirstPoint());

			final ProcessFigure sourceProcessFigure = getProcessFigure(conn
					.getSourceAnchor());
			final ProcessFigure targetProcessFigure = getProcessFigure(conn
					.getTargetAnchor());

			if (targetProcessFigure.getLocation().x < sourceProcessFigure
					.getLocation().x + sourceProcessFigure.getSize().width
					|| targetProcessFigure.getLocation().x > sourceProcessFigure
							.getLocation().x
							+ sourceProcessFigure.getSize().width
							+ GraphLayoutManager.horizontalSpacing
							+ targetProcessFigure.getSize().width
					|| targetProcessFigure == sourceProcessFigure) {
				points.addPoint(conn
						.getPoints()
						.getFirstPoint()
						.getTranslated(
								GraphLayoutManager.horizontalSpacing / 2, 0));

				final int y1 = sourceProcessFigure.getLocation().y < targetProcessFigure
						.getLocation().y ? targetProcessFigure.getLocation().y
						- GraphLayoutManager.verticalSpacing / 2
						: sourceProcessFigure.getLocation().y
								- GraphLayoutManager.verticalSpacing / 2;

				points.addPoint(
						conn.getPoints()
								.getFirstPoint()
								.getTranslated(
										+GraphLayoutManager.horizontalSpacing / 2,
										0).x, y1);

				points.addPoint(
						conn.getPoints()
								.getLastPoint()
								.getTranslated(
										-GraphLayoutManager.horizontalSpacing / 2,
										0).x, y1);

				points.addPoint(conn
						.getPoints()
						.getLastPoint()
						.getTranslated(
								-GraphLayoutManager.horizontalSpacing / 2, 0));
			} else {
				points.addPoint(conn
						.getPoints()
						.getFirstPoint()
						.getTranslated(
								GraphLayoutManager.horizontalSpacing / 2, 0));

				points.addPoint(
						conn.getPoints()
								.getFirstPoint()
								.getTranslated(
										GraphLayoutManager.horizontalSpacing / 2,
										0).x, conn.getPoints().getLastPoint().y);
			}

			points.addPoint(conn.getPoints().getLastPoint());
			conn.setPoints(points);
		}
	}

}
