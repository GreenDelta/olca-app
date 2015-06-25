package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.swt.graphics.RGB;
import org.openlca.app.FaviColor;
import org.openlca.app.results.analysis.sankey.layout.ConnectionRouterImp;
import org.openlca.app.util.Colors;

public class ConnectionLinkEditPart extends AbstractConnectionEditPart {

	private ConnectionRouter router = new ConnectionRouterImp();

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
	}

	@Override
	protected IFigure createFigure() {
		ConnectionLink link = (ConnectionLink) getModel();
		double dWidth = link.getRatio() * ConnectionLink.MAXIMIM_WIDTH;
		int width = (int) Math.ceil(Math.abs(dWidth));
		if (width == 0)
			width = 1;
		if (width > ConnectionLink.MAXIMIM_WIDTH)
			width = ConnectionLink.MAXIMIM_WIDTH;
		PolylineConnection conn = new ConnectionLinkFigure(width,
				((ProductSystemNode) link.getSourceNode().getParent())
						.getEditor());
		conn.setConnectionRouter(router);
		link.setFigure(conn);
		conn.setTolerance(0);
		RGB rgb = FaviColor.getForContribution(link.getRatio());
		conn.setForegroundColor(Colors.getColor(rgb));
		return conn;
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

}
