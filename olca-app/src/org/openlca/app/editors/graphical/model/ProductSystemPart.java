package org.openlca.app.editors.graphical.model;

import java.util.EventObject;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.openlca.app.editors.graphical.layout.Animation;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.policy.LayoutPolicy;

class ProductSystemPart extends AppAbstractEditPart<ProductSystemNode> {

	CommandStackListener stackListener = new CommandStackChangedListener();

	@Override
	public void activate() {
		super.activate();
		getViewer().getEditDomain().getCommandStack().addCommandStackListener(stackListener);
	}

	@Override
	public void deactivate() {
		getViewer().getEditDomain().getCommandStack().removeCommandStackListener(stackListener);
		super.deactivate();
	}

	@Override
	protected IFigure createFigure() {
		ProductSystemNode node = getModel();
		ProductSystemFigure figure = new ProductSystemFigure(node);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new LayoutPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
		});
		LayoutManager manager = new LayoutManager(getModel());
		getFigure().setLayoutManager(manager);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProcessPart> getChildren() {
		return super.getChildren();
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	private class CommandStackChangedListener implements CommandStackListener {

		@Override
		public void commandStackChanged(EventObject event) {
			if (!Animation.captureLayout(getFigure()))
				return;
			while (Animation.step())
				getFigure().getUpdateManager().performUpdate();
			Animation.end();
		}
	}

}
