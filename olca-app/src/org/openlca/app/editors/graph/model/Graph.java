package org.openlca.app.editors.graph.model;


/**
 * A {@link Graph} renders a system of unit processes, library
 * processes, results and/or product systems (represented by a
 * {@link Node}).
 */
public class Graph extends GraphComponent {

	public static String ID_ROUTER = "router"; //$NON-NLS-1$

	protected Integer connectionRouter = null;
	private double zoom = 1.0;

	public Integer getConnectionRouter() {
		// TODO
		//		if (connectionRouter == null)
		//			connectionRouter = ROUTER_MANUAL;
		return connectionRouter;
	}

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

	public void setConnectionRouter(Integer router) {
		Integer oldConnectionRouter = connectionRouter;
		connectionRouter = router;
		firePropertyChange(ID_ROUTER, oldConnectionRouter, connectionRouter);
	}

	public String toString() {
		return "GraphModel";
	}

}
