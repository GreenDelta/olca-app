package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.CommandStackEvent;
import org.eclipse.gef.commands.CommandStackEventListener;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.openlca.app.editors.graphical.layout.Animation;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.policy.LayoutPolicy;
import org.openlca.app.editors.graphical.view.ProductSystemFigure;

public class ProductSystemPart extends AppAbstractEditPart<ProductSystemNode> {

	CommandStackChangedListener stackListener = new CommandStackChangedListener();

	@Override
	public void activate() {
		super.activate();
		getViewer().getEditDomain().getCommandStack().addCommandStackEventListener(stackListener);
	}

	@Override
	public void deactivate() {
		getViewer().getEditDomain().getCommandStack().removeCommandStackEventListener(stackListener);
		super.deactivate();
	}

	@Override
	protected IFigure createFigure() {
		ProductSystemNode node = getModel();
		return new ProductSystemFigure(node);
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
		return true;
	}

	public void resetChildEditPart(ProcessPart processPart) {
		var index = getChildren().indexOf(processPart);
		var newProcessPart = createChild(processPart.getModel());
		removeChild(processPart);
		addChild(newProcessPart, index);
	}

	private class CommandStackChangedListener implements CommandStackEventListener {

		@Override
		public void stackChanged(CommandStackEvent event) {
			if (!Animation.captureLayout(getFigure()))
				return;
			while (Animation.step())
				getFigure().getUpdateManager().performUpdate();
			Animation.end();
		}
	}

}
