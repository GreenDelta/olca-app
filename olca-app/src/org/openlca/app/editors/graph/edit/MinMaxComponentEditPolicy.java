package org.openlca.app.editors.graph.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graph.model.MinMaxGraphComponent;
import org.openlca.app.editors.graph.model.commands.MinMaxCommand;

import static org.openlca.app.editors.graph.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graph.model.commands.MinMaxCommand.MINIMIZE;
import static org.openlca.app.editors.graph.requests.GraphRequestConstants.REQ_MAX;
import static org.openlca.app.editors.graph.requests.GraphRequestConstants.REQ_MIN;

public class MinMaxComponentEditPolicy extends GraphComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_OPEN.equals(request.getType())) {
			var child = (MinMaxGraphComponent) getHost().getModel();
			return getMinMaxCommand(child.isMinimized() ? MAXIMIZE : MINIMIZE);
		}
		if (REQ_MIN.equals(request.getType())) {
			var child = (MinMaxGraphComponent) getHost().getModel();
			if (!child.isMinimized())
				return getMinMaxCommand(MINIMIZE);
		}
		if (REQ_MAX.equals(request.getType())) {
			var child = (MinMaxGraphComponent) getHost().getModel();
			if (child.isMinimized())
				return getMinMaxCommand(MAXIMIZE);
		}
		return super.getCommand(request);
	}

	private Command getMinMaxCommand(int type) {
		var childEditPart = getHost();
		var child = (MinMaxGraphComponent) childEditPart.getModel();
		var command = new MinMaxCommand(type);
		command.setChild(child);
		return command;
	}

}
