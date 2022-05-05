package org.openlca.app.editors.graphical.policy;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graphical.command.DeleteProcessCommand;
import org.openlca.app.editors.graphical.command.MinMaxCommand;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProcessPart;
import org.openlca.app.editors.graphical.model.ProductSystemPart;

public class ProcessEditPolicy extends ComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_OPEN.equals(request.getType()))
			return getMinMaxCommand();
		return super.getCommand(request);
	}

	private Command getMinMaxCommand() {
		var processPart = (ProcessPart) getHost();
		var productSystemPart = (ProductSystemPart) getHost().getParent();
		return new MinMaxCommand(productSystemPart, processPart);
	}

	@Override
	protected Command createDeleteCommand(GroupRequest req) {
		return new DeleteProcessCommand((ProcessNode) getHost().getModel());
	}
}
