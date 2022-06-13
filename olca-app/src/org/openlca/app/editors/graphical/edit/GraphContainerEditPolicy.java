package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.commands.AddProcessCommand;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_ADD_PROCESS;

public class GraphContainerEditPolicy extends ContainerEditPolicy {

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;
	}

	@Override
	public Command getCommand(Request request) {
		if (REQ_ADD_PROCESS.equals(request.getType()))
			return getAddExchangeCommand();
		return super.getCommand(request);
	}

	private Command getAddExchangeCommand() {
		return new AddProcessCommand((Graph) getHost().getModel());
	}

}
