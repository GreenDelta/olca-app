package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.analysis.sankey.layout.ConnectionRouterImp;

public class LinkPart extends AbstractConnectionEditPart {

	public static final ConnectionRouter ROUTER = new ConnectionRouterImp();

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
	}

	@Override
	protected IFigure createFigure() {
		Link link = (Link) getModel();
		link.editPart = this;
		SankeyDiagram editor = link.sourceNode.parent.editor;
		PolylineConnection conn = new LinkFigure(link.getWidth(), editor);
		if (editor.isRouted()) {
			conn.setConnectionRouter(ROUTER);
		} else {
			conn.setConnectionRouter(ConnectionRouter.NULL);
		}
		link.figure = conn;
		conn.setTolerance(0);
		conn.setForegroundColor(link.getColor());
		return conn;
	}

	@Override
	public void setSelected(int value) {
		PolylineConnection figure = (PolylineConnection) getFigure();
		Link link = ((Link) getModel());
		if (value != EditPart.SELECTED_NONE) {
			figure.setForegroundColor(Link.HIGHLIGHT_COLOR);
		} else {
			figure.setForegroundColor(link.getColor());
		}
		super.setSelected(value);
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

}
