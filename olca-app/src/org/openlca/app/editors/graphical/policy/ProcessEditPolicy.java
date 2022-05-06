package org.openlca.app.editors.graphical.policy;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graphical.command.DeleteProcessCommand;
import org.openlca.app.editors.graphical.command.MinMaxCommand;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class ProcessEditPolicy extends ComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_OPEN.equals(request.getType()))
			return getMinMaxCommand();
		return super.getCommand(request);
	}

	private Command getMinMaxCommand() {
		return new MinMaxCommand((ProcessNode) getHost().getModel());
	}

	@Override
	protected Command createDeleteCommand(GroupRequest req) {
		return new DeleteProcessCommand((ProcessNode) getHost().getModel());
	}
}
