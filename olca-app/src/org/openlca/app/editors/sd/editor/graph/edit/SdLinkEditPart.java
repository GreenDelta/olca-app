package org.openlca.app.editors.sd.editor.graph.edit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.sd.editor.graph.figures.SdLinkFigure;
import org.openlca.app.editors.sd.editor.graph.model.SdLink;
import org.openlca.app.editors.sd.editor.graph.model.commands.DeleteLinkCommand;

/**
 * EditPart for SdLink connections.
 */
public class SdLinkEditPart extends AbstractConnectionEditPart
		implements PropertyChangeListener {

	@Override
	public void activate() {
		if (!isActive()) {
			super.activate();
			getModel().addPropertyChangeListener(this);
		}
	}

	@Override
	public void deactivate() {
		if (isActive()) {
			getModel().removePropertyChangeListener(this);
			super.deactivate();
		}
	}

	@Override
	protected IFigure createFigure() {
		var figure = new SdLinkFigure(getModel());
		// Ensure the figure is visible with a minimum line width
		figure.setLineWidth(2);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
		installEditPolicy(EditPolicy.CONNECTION_ROLE, new ConnectionEditPolicy() {
			@Override
			protected Command getDeleteCommand(GroupRequest request) {
				return new DeleteLinkCommand(getModel());
			}
		});
	}

	@Override
	public SdLink getModel() {
		return (SdLink) super.getModel();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// Refresh the figure when link properties change
		refreshVisuals();
	}

	@Override
	protected void refreshVisuals() {
		var figure = (SdLinkFigure) getFigure();
		figure.updateStyle();
	}
}
