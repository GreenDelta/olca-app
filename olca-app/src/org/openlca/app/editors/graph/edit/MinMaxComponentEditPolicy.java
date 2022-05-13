package org.openlca.app.editors.graph.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.openlca.app.editors.graph.model.MinMaxGraphComponent;
import org.openlca.app.editors.graph.model.commands.MinMaxCommand;

public class MinMaxComponentEditPolicy extends ComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_OPEN.equals(request.getType())) {
			var child = (MinMaxGraphComponent) getHost().getModel();
			return getMinMaxCommand(!child.isMinimized());
			}
		return super.getCommand(request);
	}

	private Command getMinMaxCommand(boolean minimize) {
		var childEditPart = getHost();
		var child = (MinMaxGraphComponent) childEditPart.getModel();
		var command = new MinMaxCommand(minimize);
		command.setChildEditPart(childEditPart);
		command.setChild(child);
		return command;
	}

}
