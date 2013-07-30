/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.analyze.sankey;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.swt.graphics.RGB;
import org.openlca.app.Colors;
import org.openlca.core.application.FaviColor;

/**
 * EditPart for {@link ConnectionLink}.
 * 
 * @see AbstractConnectionEditPart
 * 
 * @author Sebastian Greve
 * 
 */
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
		int width = (int) Math.ceil(Math.abs(link.getRatio()
				* ConnectionLink.MAXIMIM_WIDTH));
		if (width == 0)
			width = 1;
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
