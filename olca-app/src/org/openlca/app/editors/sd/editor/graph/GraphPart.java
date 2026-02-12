package org.openlca.app.editors.sd.editor.graph;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

class GraphPart extends AbstractGraphicalEditPart {

	@Override
	protected IFigure createFigure() {
		var figure = new FreeformLayer();
		figure.setLayoutManager(new FreeformLayout());
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
	}

	@Override
	protected List<?> getModelChildren() {
		var model = (GraphModel) getModel();
		if (model == null)
			return Collections.emptyList();
		return model.stocks;
	}

	private static class LayoutPolicy extends XYLayoutEditPolicy {
		@Override
		protected Command getCreateCommand(CreateRequest request) {
			return null;
		}

		// @Override
		// protected Command createChangeBoundsCommand(EditPart child, Object constraint) {
		//	if (child instanceof StockPart part && constraint instanceof Rectangle rect) {
		//		return new ChangeBoundsCommand((StockModel) part.getModel(), rect);
		//	}
		//	return null;
		// }
	}

}
