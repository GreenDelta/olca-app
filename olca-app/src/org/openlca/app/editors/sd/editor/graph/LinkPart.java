package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Theme.Box;

class LinkPart extends AbstractConnectionEditPart {

	private final Theme theme;

	LinkPart(LinkModel model, Theme theme) {
		setModel(model);
		this.theme = theme;
	}

	@Override
	protected IFigure createFigure() {
		var connection = new PolylineConnection();
		connection.setTargetDecoration(new PolylineDecoration());
		return connection;
	}

	@Override
	protected void createEditPolicies() {
	}

	@Override
	public LinkModel getModel() {
		return super.getModel() instanceof LinkModel link ? link : null;
	}

	@Override
	protected void refreshVisuals() {
		if (!(getFigure() instanceof PolylineConnection con)) {
			return;
		}
		var link = getModel();
		if (link == null) return;
		// we just re-use the colors from the theme here,
		// the flow types have no specific meaning
		var color = link.isStockFlow()
			? theme.boxBorderColor(Box.SUB_SYSTEM)
			: theme.boxBorderColor(Box.RESULT);
		con.setForegroundColor(color);
	}

}
