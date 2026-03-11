package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Theme.Box;
import org.openlca.app.editors.sd.editor.graph.model.VarLink;
import org.openlca.sd.model.Auxil;
import org.openlca.sd.model.Stock;

class LinkPart extends AbstractConnectionEditPart {

	private final Theme theme;

	LinkPart(VarLink model, Theme theme) {
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
	public VarLink getModel() {
		return super.getModel() instanceof VarLink link ? link : null;
	}

	@Override
	protected void refreshVisuals() {
		if (!(getFigure() instanceof PolylineConnection con)) {
			return;
		}
		var link = getModel();
		if (link == null || link.source() == null) {
			return;
		}

		// we just re-use the colors from the theme here,
		// the flow types have no specific meaning
		var color = switch (link.source().variable()) {
			case Auxil ignore -> theme.boxBorderColor(Box.RESULT);
			case Stock ignore -> theme.boxBorderColor(Box.DEFAULT);
			default -> theme.boxBorderColor(Box.SUB_SYSTEM);
		};
		con.setForegroundColor(color);
	}

}
