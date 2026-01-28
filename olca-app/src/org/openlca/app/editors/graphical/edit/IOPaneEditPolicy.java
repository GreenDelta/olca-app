package org.openlca.app.editors.graphical.edit;

import static org.openlca.app.editors.graphical.requests.GraphRequests.*;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.openlca.app.editors.graphical.model.commands.AddExchangeCommand;


public class IOPaneEditPolicy extends ContainerEditPolicy {

	@Override
	public Command getCommand(Request req) {
		if (req == null || !(req.getType() instanceof String type)) {
			return super.getCommand(req);
		}
		return switch (type) {
			case REQ_ADD_INPUT -> getAddExchangeCommand(true);
			case REQ_ADD_OUTPUT -> getAddExchangeCommand(false);
			default -> super.getCommand(req);
		};
	}

	private Command getAddExchangeCommand(boolean forInput) {
		if (!(getHost() instanceof IOPaneEditPart part)) {
			return null;
		}
		return new AddExchangeCommand(part.getModel(), forInput);
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;
	}
}
