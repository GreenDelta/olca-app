package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;

class LinkPart extends AbstractConnectionEditPart {

	@Override
	protected IFigure createFigure() {
		var connection = new PolylineConnection();
		connection.setTargetDecoration(new PolylineDecoration());
		return connection;
	}

	@Override
	protected void createEditPolicies() {
	}

}
