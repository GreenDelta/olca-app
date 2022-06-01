package org.openlca.app.editors.graph.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.openlca.app.editors.graph.model.IOPane;
import org.openlca.app.editors.graph.model.commands.AddExchangeCommand;

import static org.openlca.app.editors.graph.actions.AddExchangeAction.REQ_ADD_INPUT_EXCHANGE;
import static org.openlca.app.editors.graph.actions.AddExchangeAction.REQ_ADD_OUTPUT_EXCHANGE;

public class IOPaneEditPolicy extends ContainerEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_ADD_INPUT_EXCHANGE.equals(request.getType()))
			return getAddExchangeCommand(true);
		if (REQ_ADD_OUTPUT_EXCHANGE.equals(request.getType()))
			return getAddExchangeCommand(false);
		return super.getCommand(request);
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;
	}

	private Command getAddExchangeCommand(boolean forInput) {
		return new AddExchangeCommand((IOPane) getHost().getModel(), forInput);
	}

}
