package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.sd.editor.graph.model.NotifySupport;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.app.editors.sd.editor.graph.view.SystemFigure;

public class SystemPart extends AbstractGraphicalEditPart {

	private final Theme theme;
	private final Runnable onModelChange = this::refreshVisuals;

	SystemPart(SystemNode model, Theme theme) {
		setModel(model);
		this.theme = theme;
	}

	@Override
	protected IFigure createFigure() {
		return new SystemFigure(theme);
	}

	@Override
	public SystemNode getModel() {
		var model = super.getModel();
		return model instanceof SystemNode m ? m : null;
	}

	@Override
	public void activate() {
		super.activate();
		NotifySupport.on(getModel(), onModelChange);
	}

	@Override
	public void deactivate() {
		NotifySupport.off(getModel(), onModelChange);
		super.deactivate();
	}

	@Override
	protected void createEditPolicies() {
	}

	@Override
	protected void refreshVisuals() {
		var node = getModel();
		if (node == null)
			return;

		var figure = getFigure();
		if (figure instanceof SystemFigure f) {
			f.setBinding(node.binding());
		}

		var parent = getParent();
		if (parent instanceof AbstractGraphicalEditPart gep) {
			gep.setLayoutConstraint(this, figure, node.bounds());
		}
	}
}
