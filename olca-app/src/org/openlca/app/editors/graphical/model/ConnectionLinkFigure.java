package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.PolylineConnection;

class ConnectionLinkFigure extends PolylineConnection {

	@Override
	public void setVisible(boolean visible) {
		firePropertyChange("SELECT", true, false);
		super.setVisible(visible);
	}

}
